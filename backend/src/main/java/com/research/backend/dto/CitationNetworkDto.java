package com.research.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CitationNetworkDto {
    private List<ReferenceDto> nodes;
    private List<CitationEdge> edges;
    private Map<String, Double> pagerankScores;
    private List<ReferenceDto> coreReferences;

    @Data
    public static class CitationEdge {
        private String source;
        private String target;
        private String type;
    }

    @Data
    public static class ReferenceDto {
        private Long id;
        private String title;
        private String authors;
        private Integer year;
        private String venue;
        private Double pagerank;
    }
}
