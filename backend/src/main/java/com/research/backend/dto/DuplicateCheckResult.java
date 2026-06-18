package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResult {
    private Long paperId;
    private Double overallSimilarity;
    private String overallRiskLevel;
    private List<SimilarPaperDto> similarPapers;
    private SausageDetectionDto sausageDetection;
    private Map<String, Object> evidenceDetails;
    private LocalDateTime checkTime;
}
