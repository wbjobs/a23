package com.research.backend.service.impl;

import com.research.backend.dto.ReviewerRecommendationDto;
import com.research.backend.neo4j.entity.Keyword;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.neo4j.repository.KeywordRepository;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.service.ReviewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewerServiceImpl implements ReviewerService {

    private final ReviewerRepository reviewerRepository;
    private final PaperRepository paperRepository;
    private final KeywordRepository keywordRepository;

    @Override
    public List<ReviewerRecommendationDto> recommendReviewers(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<Keyword> paperKeywords = paper.getKeywords() != null && !paper.getKeywords().isEmpty() ?
                paper.getKeywords() : keywordRepository.findKeywordsByPaperId(paperId);

        Set<String> paperKeywordNames = new HashSet<>();
        for (Keyword kw : paperKeywords) {
            paperKeywordNames.add(kw.getName().toLowerCase());
        }

        List<Reviewer> allReviewers = reviewerRepository.findAll();
        List<ReviewerRecommendationDto> recommendations = new ArrayList<>();

        for (Reviewer reviewer : allReviewers) {
            double similarity = calculateCosineSimilarity(paperKeywordNames, reviewer);
            recommendations.add(new ReviewerRecommendationDto(reviewer, similarity));
        }

        recommendations.sort((a, b) -> b.getSimilarityScore().compareTo(a.getSimilarityScore()));

        return recommendations.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(Set<String> paperKeywords, Reviewer reviewer) {
        Set<String> reviewerKeywords = new HashSet<>();

        if (reviewer.getExpertKeywords() != null) {
            for (Keyword kw : reviewer.getExpertKeywords()) {
                reviewerKeywords.add(kw.getName().toLowerCase());
            }
        }

        if (reviewer.getResearchAreas() != null && !reviewer.getResearchAreas().isEmpty()) {
            String[] areas = reviewer.getResearchAreas().split("[,;，；\\s]+");
            for (String area : areas) {
                String trimmed = area.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    reviewerKeywords.add(trimmed);
                }
            }
        }

        Map<String, Integer> paperVector = new HashMap<>();
        for (String kw : paperKeywords) {
            paperVector.put(kw, 1);
        }

        Map<String, Integer> reviewerVector = new HashMap<>();
        for (String kw : reviewerKeywords) {
            reviewerVector.put(kw, 1);
        }

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(paperVector.keySet());
        allTerms.addAll(reviewerVector.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : allTerms) {
            int a = paperVector.getOrDefault(term, 0);
            int b = reviewerVector.getOrDefault(term, 0);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public Reviewer getReviewerById(Long id) {
        return reviewerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("审稿人不存在"));
    }

    @Override
    public List<Reviewer> getAllReviewers() {
        return reviewerRepository.findAll();
    }

    @Override
    public Reviewer saveReviewer(Reviewer reviewer) {
        return reviewerRepository.save(reviewer);
    }
}
