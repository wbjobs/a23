package com.research.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.backend.dto.*;
import com.research.backend.entity.ReviewRecord;
import com.research.backend.entity.ReviewReply;
import com.research.backend.entity.User;
import com.research.backend.neo4j.entity.Author;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.repository.ReviewRecordRepository;
import com.research.backend.repository.ReviewReplyRepository;
import com.research.backend.repository.UserRepository;
import com.research.backend.service.EmailService;
import com.research.backend.service.NlpService;
import com.research.backend.service.ReviewReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewReplyServiceImpl implements ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final NlpService nlpService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final Neo4jClient neo4jClient;

    @Override
    @Transactional
    public ReviewReplyDto submitReply(ReviewReplyRequest request, Long authorId) {
        ReviewRecord record = reviewRecordRepository.findById(request.getReviewRecordId())
                .orElseThrow(() -> new RuntimeException("评审记录不存在"));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Paper paper = paperRepository.findById(record.getPaperId())
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        Integer threadNumber = request.getThreadNumber();
        if (threadNumber == null) {
            List<ReviewReply> existingReplies = reviewReplyRepository
                    .findByReviewRecordIdOrderByThreadNumberAscCreateTimeAsc(request.getReviewRecordId());
            Set<Integer> existingThreads = existingReplies.stream()
                    .map(ReviewReply::getThreadNumber)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            threadNumber = existingThreads.isEmpty() ? 1 : Collections.max(existingThreads) + 1;
        }

        List<SuggestedReferenceDto> suggestedRefs = suggestRelatedReferences(
                record.getPaperId(), request.getContent());

        String suggestedRefsJson = null;
        String citedLitJson = null;
        try {
            suggestedRefsJson = objectMapper.writeValueAsString(suggestedRefs);
            if (request.getCitedLiterature() != null && !request.getCitedLiterature().isEmpty()) {
                List<ReferenceDto> citedLit = new ArrayList<>();
                for (String ref : request.getCitedLiterature()) {
                    citedLit.add(ReferenceDto.builder().title(ref).build());
                }
                citedLitJson = objectMapper.writeValueAsString(citedLit);
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }

        ReviewReply reply = ReviewReply.builder()
                .reviewRecordId(request.getReviewRecordId())
                .paperId(record.getPaperId())
                .authorId(authorId)
                .authorName(author.getName())
                .threadNumber(threadNumber)
                .parentReplyId(request.getParentReplyId())
                .content(request.getContent())
                .suggestedReferencesJson(suggestedRefsJson)
                .citedLiteratureJson(citedLitJson)
                .status(ReviewReply.ReplyStatus.SUBMITTED)
                .resolved(false)
                .build();

        ReviewReply saved = reviewReplyRepository.save(reply);

        try {
            User reviewer = userRepository.findById(record.getReviewerId()).orElse(null);
            if (reviewer != null && reviewer.getEmail() != null) {
                emailService.sendReplyNotification(
                        reviewer.getEmail(),
                        reviewer.getName(),
                        paper,
                        author.getName(),
                        saved.getContent()
                );
            }
        } catch (Exception ignored) {
        }

        return convertToDto(saved);
    }

    @Override
    public List<ReplyThreadDto> getReplyThreads(Long reviewRecordId) {
        ReviewRecord record = reviewRecordRepository.findById(reviewRecordId)
                .orElseThrow(() -> new RuntimeException("评审记录不存在"));

        List<ReviewReply> replies = reviewReplyRepository
                .findByReviewRecordIdOrderByThreadNumberAscCreateTimeAsc(reviewRecordId);

        Map<Integer, List<ReviewReply>> groupedByThread = replies.stream()
                .filter(r -> r.getThreadNumber() != null)
                .collect(Collectors.groupingBy(ReviewReply::getThreadNumber));

        List<ReplyThreadDto> threads = new ArrayList<>();
        for (Map.Entry<Integer, List<ReviewReply>> entry : groupedByThread.entrySet()) {
            Integer threadNum = entry.getKey();
            List<ReviewReply> threadReplies = entry.getValue();

            boolean resolved = threadReplies.stream()
                    .allMatch(r -> Boolean.TRUE.equals(r.getResolved()));

            String originalComment = extractCommentSegment(record.getComments(), threadNum);

            List<ReviewReplyDto> replyDtos = threadReplies.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            threads.add(ReplyThreadDto.builder()
                    .threadNumber(threadNum)
                    .originalComment(originalComment)
                    .replies(replyDtos)
                    .resolved(resolved)
                    .build());
        }

        threads.sort(Comparator.comparingInt(ReplyThreadDto::getThreadNumber));
        return threads;
    }

    @Override
    public List<ReviewReplyDto> getRepliesByThread(Long reviewRecordId, Integer threadNumber) {
        List<ReviewReply> replies = reviewReplyRepository
                .findByReviewRecordIdAndThreadNumberOrderByCreateTimeAsc(reviewRecordId, threadNumber);
        return replies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestedReferenceDto> suggestRelatedReferences(Long paperId, String content) {
        List<String> keywords = nlpService.extractKeywords(content, content);
        List<String> datasets = nlpService.extractDatasets(content, content);

        Paper currentPaper = paperRepository.findById(paperId).orElse(null);
        if (currentPaper != null && currentPaper.getKeywords() != null) {
            for (var kw : currentPaper.getKeywords()) {
                if (kw.getName() != null && !keywords.contains(kw.getName())) {
                    keywords.add(kw.getName());
                }
            }
        }

        String cypher = "MATCH (p:Paper) " +
                "WHERE id(p) <> $paperId " +
                "OPTIONAL MATCH (p)-[:USES_DATASET]->(d:Dataset) " +
                "OPTIONAL MATCH (p)-[:HAS_KEYWORD]->(k:Keyword) " +
                "WITH p, collect(DISTINCT d.name) as datasets, collect(DISTINCT k.name) as keywords " +
                "WITH p, datasets, keywords, " +
                "     size([x IN datasets WHERE x IN $targetDatasets]) as datasetMatches, " +
                "     size([x IN keywords WHERE x IN $targetKeywords]) as keywordMatches " +
                "WHERE datasetMatches > 0 OR keywordMatches > 0 " +
                "WITH p, datasetMatches, keywordMatches, " +
                "     (datasetMatches * 2 + keywordMatches) as totalScore " +
                "ORDER BY totalScore DESC " +
                "LIMIT 5 " +
                "RETURN p, totalScore, datasetMatches, keywordMatches";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("paperId", paperId);
        parameters.put("targetDatasets", datasets);
        parameters.put("targetKeywords", keywords);

        List<SuggestedReferenceDto> results = new ArrayList<>();

        try {
            Collection<Map<String, Object>> queryResults = neo4jClient
                    .query(cypher)
                    .bindAll(parameters)
                    .fetchAs(Map.class)
                    .all()
                    .block();

            if (queryResults != null) {
                int rank = 1;
                for (Map<String, Object> row : queryResults) {
                    Paper p = (Paper) row.get("p");
                    Long totalScore = (Long) row.get("totalScore");
                    Long datasetMatches = (Long) row.get("datasetMatches");
                    Long keywordMatches = (Long) row.get("keywordMatches");

                    StringBuilder matchReason = new StringBuilder();
                    if (datasetMatches > 0) {
                        matchReason.append("匹配").append(datasetMatches).append("个数据集");
                    }
                    if (keywordMatches > 0) {
                        if (matchReason.length() > 0) matchReason.append("，");
                        matchReason.append("匹配").append(keywordMatches).append("个关键词");
                    }

                    double confidence = Math.min(1.0, totalScore.doubleValue() / 10.0);

                    List<Author> authors = p.getAuthors();
                    String authorsStr = "";
                    if (authors != null && !authors.isEmpty()) {
                        authorsStr = authors.stream()
                                .map(Author::getName)
                                .collect(Collectors.joining(", "));
                    }

                    results.add(SuggestedReferenceDto.builder()
                            .id(p.getId())
                            .title(p.getTitle())
                            .authors(authorsStr)
                            .abstractSnippet(p.getAbstractText() != null && p.getAbstractText().length() > 200
                                    ? p.getAbstractText().substring(0, 200) + "..."
                                    : p.getAbstractText())
                            .similarityScore(totalScore.doubleValue())
                            .confidence(confidence)
                            .matchReason(matchReason.toString())
                            .build());
                    rank++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询知识图谱失败", e);
        }

        return results;
    }

    @Override
    @Transactional
    public ReviewReplyDto addReviewerResponse(Long replyId, Long reviewerId, String response) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("回复不存在"));

        ReviewRecord record = reviewRecordRepository.findById(reply.getReviewRecordId())
                .orElseThrow(() -> new RuntimeException("评审记录不存在"));

        if (!record.getReviewerId().equals(reviewerId)) {
            throw new RuntimeException("无权限回应此回复");
        }

        reply.setReviewerResponse(response);
        reply.setReviewerResponseTime(LocalDateTime.now());
        ReviewReply saved = reviewReplyRepository.save(reply);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public boolean markResolved(Long replyId, Long userId) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("回复不存在"));

        ReviewRecord record = reviewRecordRepository.findById(reply.getReviewRecordId())
                .orElseThrow(() -> new RuntimeException("评审记录不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        boolean hasPermission = "ADMIN".equals(user.getRole().name().replace("ROLE_", ""))
                || "EDITOR".equals(user.getRole().name().replace("ROLE_", ""))
                || record.getReviewerId().equals(userId)
                || reply.getAuthorId().equals(userId);

        if (!hasPermission) {
            throw new RuntimeException("无权限标记此回复");
        }

        reply.setResolved(true);
        reply.setStatus(ReviewReply.ReplyStatus.RESOLVED);
        reviewReplyRepository.save(reply);
        return true;
    }

    @Override
    public List<ReviewReplyDto> getAuthorReplies(Long authorId, Long paperId) {
        List<ReviewReply> replies;
        if (paperId != null) {
            replies = reviewReplyRepository.findByPaperIdAndAuthorIdOrderByCreateTimeDesc(paperId, authorId);
        } else {
            replies = reviewReplyRepository.findAll().stream()
                    .filter(r -> r.getAuthorId() != null && r.getAuthorId().equals(authorId))
                    .sorted(Comparator.comparing(ReviewReply::getCreateTime).reversed())
                    .collect(Collectors.toList());
        }
        return replies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ReviewReplyDto convertToDto(ReviewReply reply) {
        List<ReferenceDto> suggestedRefs = parseReferencesJson(reply.getSuggestedReferencesJson());
        List<ReferenceDto> citedLit = parseReferencesJson(reply.getCitedLiteratureJson());

        return ReviewReplyDto.builder()
                .id(reply.getId())
                .reviewRecordId(reply.getReviewRecordId())
                .threadNumber(reply.getThreadNumber())
                .parentReplyId(reply.getParentReplyId())
                .content(reply.getContent())
                .suggestedReferences(suggestedRefs)
                .citedLiterature(citedLit)
                .authorName(reply.getAuthorName())
                .createTime(reply.getCreateTime())
                .status(reply.getStatus())
                .resolved(reply.getResolved())
                .reviewerResponse(reply.getReviewerResponse())
                .reviewerResponseTime(reply.getReviewerResponseTime())
                .build();
    }

    private List<ReferenceDto> parseReferencesJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ReferenceDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String extractCommentSegment(String comments, Integer threadNumber) {
        if (comments == null || comments.isEmpty() || threadNumber == null) {
            return "";
        }
        String[] segments = comments.split("\\n\\s*\\d+\\.");
        if (segments.length > threadNumber) {
            return segments[threadNumber].trim();
        }
        if (segments.length > 1) {
            int idx = Math.min(threadNumber, segments.length - 1);
            return segments[idx].trim();
        }
        return comments;
    }
}
