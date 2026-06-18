package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentsQualityDto {

    private Double averageWordCount;
    private Double structureScore;
    private Double detailLevel;
    private Double actionableAdviceRate;
}
