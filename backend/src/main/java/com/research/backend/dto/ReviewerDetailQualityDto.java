package com.research.backend.dto;

import com.research.backend.entity.ReviewQualityMetrics;
import com.research.backend.neo4j.entity.Reviewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDetailQualityDto {

    private Reviewer reviewer;
    private ReviewQualityMetrics latestMetrics;
    private List<ReviewRecordQualityDto> reviews;
    private CommentsQualityDto commentsQualityAnalysis;
}
