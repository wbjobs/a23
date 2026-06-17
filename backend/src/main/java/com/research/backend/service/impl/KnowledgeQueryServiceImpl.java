package com.research.backend.service.impl;

import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.repository.KeywordRepository;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.service.KnowledgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryServiceImpl implements KnowledgeQueryService {

    private final PaperRepository paperRepository;
    private final ReviewerRepository reviewerRepository;
    private final KeywordRepository keywordRepository;

    @Override
    public Map<String, Object> queryGraph(Long paperId) {
        Map<String, Object> result = new HashMap<>();

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        result.put("paper", paper);
        result.put("keywords", keywordRepository.findKeywordsByPaperId(paperId));
        result.put("recommendedReviewers", reviewerRepository.findAll());
        result.put("similarPapersCount", paperRepository.findSimilarPapers(paperId).size());

        return result;
    }

    @Override
    public List<Paper> findSimilarPapers(Long paperId) {
        if (!paperRepository.existsById(paperId)) {
            throw new RuntimeException("论文不存在");
        }
        return paperRepository.findSimilarPapers(paperId);
    }
}
