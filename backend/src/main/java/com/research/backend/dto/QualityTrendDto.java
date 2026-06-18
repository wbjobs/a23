package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityTrendDto {

    private String month;
    private Double averageDuration;
    private Double averageScore;
    private Double timelyRate;
}
