package com.research.backend.service;

import com.research.backend.neo4j.entity.Paper;

import java.util.List;
import java.util.Map;

public interface KnowledgeQueryService {
    Map<String, Object> queryGraph(Long paperId);
    List<Paper> findSimilarPapers(Long paperId);
}
