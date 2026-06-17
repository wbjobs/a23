package com.research.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TopicDto {
    private Long id;
    private String name;
    private Double weight;
    private Boolean isCore;
    private String source;
    private List<String> topicWords;
    private List<String> representativeDocs;
}
