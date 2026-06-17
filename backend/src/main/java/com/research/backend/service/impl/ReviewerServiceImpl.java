package com.research.backend.service.impl;

import com.research.backend.dto.ReviewerRecommendationDto;
import com.research.backend.neo4j.entity.Keyword;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.neo4j.entity.Topic;
import com.research.backend.neo4j.repository.KeywordRepository;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.neo4j.repository.TopicRepository;
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
    private final TopicRepository topicRepository;

    @Override
    public List<ReviewerRecommendationDto> recommendReviewers(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        Map<String, Double> paperTopicWeights = getPaperTopicWeights(paperId);

        List<Reviewer> allReviewers = reviewerRepository.findAll();
        List<ReviewerRecommendationDto> recommendations = new ArrayList<>();

        for (Reviewer reviewer : allReviewers) {
            double similarity = calculateWeightedCosineSimilarity(paperTopicWeights, reviewer);
            recommendations.add(new ReviewerRecommendationDto(reviewer, similarity));
        }

        recommendations.sort((a, b) -> b.getSimilarityScore().compareTo(a.getSimilarityScore()));

        return recommendations.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    private Map<String, Double> getPaperTopicWeights(Long paperId) {
        Map<String, Double> topicWeights = new HashMap<>();

        List<Topic> coreTopics = topicRepository.findCoreTopicsByPaperId(paperId);
        if (coreTopics != null && !coreTopics.isEmpty()) {
            for (Topic topic : coreTopics) {
                double weight = topic.getWeight() != null ? topic.getWeight() : 1.0;
                topicWeights.put(topic.getName().toLowerCase(), weight * 2.0);
            }
        }

        List<Topic> allTopics = topicRepository.findByPaperId(paperId);
        if (allTopics != null && !allTopics.isEmpty()) {
            for (Topic topic : allTopics) {
                String name = topic.getName().toLowerCase();
                if (!topicWeights.containsKey(name)) {
                    double weight = topic.getWeight() != null ? topic.getWeight() : 0.5;
                    topicWeights.put(name, weight);
                }
            }
        }

        if (topicWeights.isEmpty()) {
            List<Keyword> paperKeywords = keywordRepository.findKeywordsByPaperId(paperId);
            if (paperKeywords != null && !paperKeywords.isEmpty()) {
                for (int i = 0; i < paperKeywords.size(); i++) {
                    double weight = 1.0 - (i * 0.05);
                    topicWeights.put(paperKeywords.get(i).getName().toLowerCase(), Math.max(weight, 0.3));
                }
            }
        }

        return topicWeights;
    }

    private double calculateWeightedCosineSimilarity(Map<String, Double> paperTopicWeights, Reviewer reviewer) {
        Map<String, Double> reviewerKeywordWeights = new HashMap<>();

        if (reviewer.getExpertKeywords() != null) {
            for (Keyword kw : reviewer.getExpertKeywords()) {
                reviewerKeywordWeights.put(kw.getName().toLowerCase(), 1.5);
            }
        }

        if (reviewer.getResearchAreas() != null && !reviewer.getResearchAreas().isEmpty()) {
            String[] areas = reviewer.getResearchAreas().split("[,;，；\\s]+");
            for (String area : areas) {
                String trimmed = area.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    reviewerKeywordWeights.put(trimmed, 1.0);
                }
            }
        }

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(paperTopicWeights.keySet());
        allTerms.addAll(reviewerKeywordWeights.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : allTerms) {
            double a = paperTopicWeights.getOrDefault(term, 0.0);
            double b = reviewerKeywordWeights.getOrDefault(term, 0.0);
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
