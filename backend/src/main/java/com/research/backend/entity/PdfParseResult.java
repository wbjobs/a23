package com.research.backend.entity;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PdfParseResult {
    private String title;
    private String abstractText;
    private List<String> authors;
    private List<String> keywords;
    private String fullText;
    private Map<String, Object> metadata;
    private List<Map<String, Object>> formulaAnchors;
    private List<Map<String, Object>> topics;
    private List<Map<String, Object>> coreTopics;
    private List<Map<String, Object>> citations;
    private Map<String, Object> citationNetwork;
}
