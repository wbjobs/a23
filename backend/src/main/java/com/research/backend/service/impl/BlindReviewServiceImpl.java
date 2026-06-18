package com.research.backend.service.impl;

import com.research.backend.config.BlindReviewConfig;
import com.research.backend.dto.BlindReviewConfigDto;
import com.research.backend.dto.PaperDesensitizedDto;
import com.research.backend.entity.ReviewRecord;
import com.research.backend.entity.User;
import com.research.backend.neo4j.entity.Author;
import com.research.backend.neo4j.entity.Institution;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.repository.ReviewRecordRepository;
import com.research.backend.repository.UserRepository;
import com.research.backend.service.BlindReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlindReviewServiceImpl implements BlindReviewService {

    private final PaperRepository paperRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final UserRepository userRepository;
    private final BlindReviewConfig blindReviewConfig;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern FUND_PATTERN = Pattern.compile("(项目|基金|资助|Grant|Fund|Project).{0,20}\\d{3,}");

    @Override
    public PaperDesensitizedDto desensitizePaper(Long paperId, Long viewerId, String viewerRole) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        boolean hasOriginalAccess = hasOriginalAccess(viewerRole, viewerId, paper);

        boolean blindEnabled = Boolean.TRUE.equals(paper.getBlindReviewEnabled()) != null
                ? paper.getBlindReviewEnabled()
                : blindReviewConfig.isEnabledDefault();

        if (!blindEnabled || hasOriginalAccess) {
            String authorsStr = extractAuthorsStr(paper);
            String affiliationStr = extractAffiliationStr(paper);
            return PaperDesensitizedDto.builder()
                    .paperId(paper.getId())
                    .desensitizedTitle(paper.getTitle())
                    .desensitizedAbstract(paper.getAbstractText())
                    .desensitizedAuthors(authorsStr)
                    .desensitizedAffiliation(affiliationStr)
                    .hasOriginalAccess(true)
                    .build();
        }

        Set<String> sensitiveWords = collectSensitiveWords(paper);

        String desensitizedAbstract = desensitizeTextWithWords(paper.getAbstractText(), sensitiveWords);

        return PaperDesensitizedDto.builder()
                .paperId(paper.getId())
                .desensitizedTitle(paper.getDesensitizedTitle() != null ? paper.getDesensitizedTitle() : desensitizeTextWithWords(paper.getTitle(), sensitiveWords))
                .desensitizedAbstract(desensitizedAbstract)
                .desensitizedAuthors("匿名作者")
                .desensitizedAffiliation("匿名机构")
                .hasOriginalAccess(false)
                .build();
    }

    @Override
    public ReviewRecord desensitizeReviewRecord(ReviewRecord record, Long viewerId, String viewerRole) {
        if (record == null) {
            return null;
        }

        Paper paper = paperRepository.findById(record.getPaperId()).orElse(null);
        boolean blindEnabled = paper != null && Boolean.TRUE.equals(paper.getBlindReviewEnabled()) != null
                ? paper.getBlindReviewEnabled()
                : blindReviewConfig.isEnabledDefault();

        boolean isAdmin = "ADMIN".equals(viewerRole);
        boolean isReviewer = viewerId != null && viewerId.equals(record.getReviewerId());

        if (!blindEnabled || isAdmin || isReviewer) {
            return record;
        }

        ReviewRecord desensitized = new ReviewRecord();
        desensitized.setId(record.getId());
        desensitized.setPaperId(record.getPaperId());
        desensitized.setReviewerId(record.getReviewerId());
        desensitized.setInnovationScore(record.getInnovationScore());
        desensitized.setMethodScore(record.getMethodScore());
        desensitized.setWritingScore(record.getWritingScore());
        desensitized.setOverallScore(record.getOverallScore());
        desensitized.setComments(record.getComments());
        desensitized.setAssignTime(record.getAssignTime());
        desensitized.setReviewTime(record.getReviewTime());
        desensitized.setStatus(record.getStatus());
        desensitized.setAnonymous(record.getAnonymous());
        desensitized.setReviewerAlias(record.getReviewerAlias());
        desensitized.setAuthorInfoDesensitized(true);

        if (Boolean.TRUE.equals(record.getAnonymous()) && record.getReviewerAlias() != null) {
            desensitized.setReviewerName(record.getReviewerAlias());
        } else if (record.getReviewerAlias() != null) {
            desensitized.setReviewerName(record.getReviewerAlias());
        } else {
            desensitized.setReviewerName("匿名评审人");
        }

        return desensitized;
    }

    @Override
    public List<ReviewRecord> desensitizeReviewList(List<ReviewRecord> records, Long viewerId, String viewerRole) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        List<ReviewRecord> result = new ArrayList<>();
        for (ReviewRecord record : records) {
            result.add(desensitizeReviewRecord(record, viewerId, viewerRole));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean toggleBlindReview(Long paperId, boolean enabled) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));
        paper.setBlindReviewEnabled(enabled);
        paperRepository.save(paper);
        return true;
    }

    @Override
    @Transactional
    public void assignReviewerAlias(Long paperId, Long reviewerId) {
        ReviewRecord record = reviewRecordRepository.findByPaperIdAndReviewerId(paperId, reviewerId)
                .orElseThrow(() -> new RuntimeException("评审记录不存在"));

        List<ReviewRecord> existingRecords = reviewRecordRepository.findByPaperId(paperId);
        int maxNumber = 0;
        for (ReviewRecord r : existingRecords) {
            if (r.getReviewerAlias() != null) {
                String alias = r.getReviewerAlias();
                String numStr = alias.replaceAll("\\D", "");
                if (!numStr.isEmpty()) {
                    int num = Integer.parseInt(numStr);
                    if (num > maxNumber) {
                        maxNumber = num;
                    }
                }
            }
        }

        String alias = String.format(blindReviewConfig.getReviewerAliasPattern(), maxNumber + 1);
        record.setReviewerAlias(alias);
        record.setAnonymous(true);
        reviewRecordRepository.save(record);
    }

    @Override
    public String desensitizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL_PATTERN.matcher(text).replaceAll("[邮箱已脱敏]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[电话已脱敏]");
        result = FUND_PATTERN.matcher(result).replaceAll("[基金信息已脱敏]");
        return result;
    }

    @Override
    public BlindReviewConfigDto getGlobalConfig() {
        BlindReviewConfigDto dto = new BlindReviewConfigDto();
        dto.setEnabled(blindReviewConfig.isEnabledDefault());
        dto.setAutoDesensitize(blindReviewConfig.isAutoDesensitize());
        dto.setDesensitizeFields(blindReviewConfig.getDesensitizeFields());
        return dto;
    }

    @Override
    @Transactional
    public BlindReviewConfigDto updateGlobalConfig(BlindReviewConfigDto configDto) {
        blindReviewConfig.setEnabledDefault(configDto.isEnabled());
        blindReviewConfig.setAutoDesensitize(configDto.isAutoDesensitize());
        if (configDto.getDesensitizeFields() != null) {
            blindReviewConfig.setDesensitizeFields(configDto.getDesensitizeFields());
        }
        return getGlobalConfig();
    }

    private boolean hasOriginalAccess(String viewerRole, Long viewerId, Paper paper) {
        if ("ADMIN".equals(viewerRole) || "EDITOR".equals(viewerRole)) {
            return true;
        }
        if (viewerId != null && paper.getSubmitterId() != null && viewerId.equals(paper.getSubmitterId())) {
            return true;
        }
        if (viewerId != null && paper.getAuthors() != null) {
            for (Author author : paper.getAuthors()) {
                User user = userRepository.findByName(author.getName()).orElse(null);
                if (user != null && viewerId.equals(user.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> collectSensitiveWords(Paper paper) {
        Set<String> sensitiveWords = new HashSet<>();
        if (paper.getAuthors() != null) {
            for (Author author : paper.getAuthors()) {
                if (author.getName() != null) {
                    sensitiveWords.add(author.getName());
                }
                if (author.getInstitution() != null && author.getInstitution().getName() != null) {
                    sensitiveWords.add(author.getInstitution().getName());
                }
            }
        }
        if (paper.getAuthorName() != null) {
            sensitiveWords.add(paper.getAuthorName());
        }
        return sensitiveWords;
    }

    private String desensitizeTextWithWords(String text, Set<String> sensitiveWords) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : sensitiveWords) {
            if (word != null && !word.isEmpty()) {
                result = result.replaceAll("(?i)" + Pattern.quote(word), "[已脱敏]");
            }
        }
        return desensitizeText(result);
    }

    private String extractAuthorsStr(Paper paper) {
        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            return paper.getAuthorName() != null ? paper.getAuthorName() : "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paper.getAuthors().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paper.getAuthors().get(i).getName());
        }
        return sb.toString();
    }

    private String extractAffiliationStr(Paper paper) {
        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            return "";
        }
        Set<String> affiliations = new HashSet<>();
        for (Author author : paper.getAuthors()) {
            Institution inst = author.getInstitution();
            if (inst != null && inst.getName() != null) {
                affiliations.add(inst.getName());
            }
        }
        return String.join(", ", affiliations);
    }
}
