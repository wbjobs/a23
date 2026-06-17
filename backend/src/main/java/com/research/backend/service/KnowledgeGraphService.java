package com.research.backend.service;

import com.research.backend.neo4j.entity.Paper;
import com.research.backend.entity.PdfParseResult;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphService {
    void buildPaperGraph(Long paperId, PdfParseResult parseResult);
    Map<String, List<String>> extractEntities(String text);
    Map<String, Object> getPaperGraph(Long paperId);
}
