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
}
