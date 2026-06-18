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
public class SharedDatasetGroupDto {
    private String datasetName;
    private Integer paperCount;
    private List<Long> paperList;
    private Double averageSimilarity;
}
