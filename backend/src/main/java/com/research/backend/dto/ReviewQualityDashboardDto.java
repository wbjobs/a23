package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQualityDashboardDto {

    private Integer totalReviewers;
    private Integer totalPapers;
    private Integer totalReviews;

    private Double overallAverageDuration;
    private Double overallTimelyRate;
    private Double overallScoreDeviation;

    private List<ReviewerRankingDto> reviewerRankings;
    private Map<String, Integer> durationDistribution;
    private Map<String, Integer> scoreDistribution;
    private List<QualityTrendDto> trendData;
}
