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
public class SausageDetectionDto {
    private Boolean isSuspected;
    private Integer suspectedPaperCount;
    private List<SharedDatasetGroupDto> sharedDatasetGroups;
    private Map<String, Object> evidence;
    private String description;
}
