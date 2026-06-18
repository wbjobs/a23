package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerRankingDto {

    private Long reviewerId;
    private String reviewerName;
    private String affiliation;
    private Integer reviewCount;
    private Double averageDuration;
    private Double timelyRate;
    private Double scoreDeviation;
    private Double overallQualityScore;
    private String qualityLevel;
    private Integer rank;
}
