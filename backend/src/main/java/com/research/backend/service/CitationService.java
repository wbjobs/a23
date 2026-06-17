package com.research.backend.service;

import com.research.backend.dto.CitationDto;
import com.research.backend.dto.CitationNetworkDto;
import com.research.backend.neo4j.entity.Reference;

import java.util.List;
import java.util.Map;

public interface CitationService {
    CitationNetworkDto getCitationNetwork(Long paperId);
    Map<String, Double> getPagerankScores(Long paperId);
    List<Reference> getCoreReferences(Long paperId, int topK);
    CitationNetworkDto buildCitationNetwork(Long paperId, List<CitationDto> citations);
}
