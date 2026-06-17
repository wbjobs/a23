package com.research.backend.dto;

import com.research.backend.neo4j.entity.Reviewer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerRecommendationDto {
    private Reviewer reviewer;
    private Double similarityScore;
}
