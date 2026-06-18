package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarPaperDto {
    private Long paperId;
    private String title;
    private String authors;
    private Double similarityScore;
    private String similarityType;
    private List<String> overlappingDatasets;
    private List<String> overlappingKeywords;
    private String riskLevel;
}
