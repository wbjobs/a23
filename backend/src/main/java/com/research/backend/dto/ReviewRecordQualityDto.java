package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecordQualityDto {

    private Long id;
    private Long paperId;
    private String paperTitle;
    private Integer innovationScore;
    private Integer methodScore;
    private Integer writingScore;
    private Integer overallScore;
    private Double reviewDurationHours;
    private Boolean timely;
    private Integer commentLength;
    private Double deviationFromMean;
}
