package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.service.KnowledgeGraphService;
import com.research.backend.service.KnowledgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kg")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeQueryService knowledgeQueryService;

    @GetMapping("/paper/{id}/graph")
    public Result<Map<String, Object>> getPaperGraph(@PathVariable Long id) {
        Map<String, Object> graph = knowledgeGraphService.getPaperGraph(id);
        return Result.success(graph);
    }

    @GetMapping("/similar/{paperId}")
    public Result<List<Paper>> findSimilarPapers(@PathVariable Long paperId) {
        List<Paper> similarPapers = knowledgeQueryService.findSimilarPapers(paperId);
        return Result.success(similarPapers);
    }

    @GetMapping("/paper/{id}/query")
    public Result<Map<String, Object>> queryGraph(@PathVariable Long id) {
        Map<String, Object> result = knowledgeQueryService.queryGraph(id);
        return Result.success(result);
    }

    @PostMapping("/extract/entities")
    public Result<Map<String, List<String>>> extractEntities(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        Map<String, List<String>> entities = knowledgeGraphService.extractEntities(text);
        return Result.success(entities);
    }
}
