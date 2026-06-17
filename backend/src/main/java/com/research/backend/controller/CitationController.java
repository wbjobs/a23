package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.CitationNetworkDto;
import com.research.backend.neo4j.entity.Reference;
import com.research.backend.service.CitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citation")
@RequiredArgsConstructor
public class CitationController {

    private final CitationService citationService;

    @GetMapping("/paper/{paperId}/network")
    public Result<CitationNetworkDto> getCitationNetwork(@PathVariable Long paperId) {
        CitationNetworkDto network = citationService.getCitationNetwork(paperId);
        return Result.success(network);
    }

    @GetMapping("/paper/{paperId}/core")
    public Result<List<Reference>> getCoreReferences(@PathVariable Long paperId,
                                                     @RequestParam(defaultValue = "5") int topK) {
        List<Reference> coreRefs = citationService.getCoreReferences(paperId, topK);
        return Result.success(coreRefs);
    }

    @GetMapping("/paper/{paperId}/pagerank")
    public Result<Map<String, Double>> getPagerankScores(@PathVariable Long paperId) {
        Map<String, Double> scores = citationService.getPagerankScores(paperId);
        return Result.success(scores);
    }
}
