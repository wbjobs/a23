package com.research.backend.service.impl;

import com.research.backend.dto.CitationDto;
import com.research.backend.dto.CitationNetworkDto;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reference;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.ReferenceRepository;
import com.research.backend.service.CitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitationServiceImpl implements CitationService {

    private final ReferenceRepository referenceRepository;
    private final PaperRepository paperRepository;

    @Override
    public CitationNetworkDto getCitationNetwork(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<Reference> references = referenceRepository.findByPaperId(paperId);

        CitationNetworkDto networkDto = new CitationNetworkDto();
        List<CitationNetworkDto.ReferenceDto> nodes = new ArrayList<>();
        List<CitationNetworkDto.CitationEdge> edges = new ArrayList<>();
        Map<String, Double> pagerankScores = new HashMap<>();

        CitationNetworkDto.ReferenceDto paperNode = new CitationNetworkDto.ReferenceDto();
        paperNode.setId(paper.getId());
        paperNode.setTitle(paper.getTitle());
        nodes.add(paperNode);

        for (Reference ref : references) {
            CitationNetworkDto.ReferenceDto refNode = convertToReferenceDto(ref);
            nodes.add(refNode);

            CitationNetworkDto.CitationEdge edge = new CitationNetworkDto.CitationEdge();
            edge.setSource("paper_" + paper.getId());
            edge.setTarget("ref_" + ref.getId());
            edge.setType("CITES");
            edges.add(edge);

            if (ref.getPagerank() != null) {
                pagerankScores.put(ref.getTitle(), ref.getPagerank());
            }
        }

        networkDto.setNodes(nodes);
        networkDto.setEdges(edges);
        networkDto.setPagerankScores(pagerankScores);

        List<Reference> coreRefs = getCoreReferences(paperId, 5);
        List<CitationNetworkDto.ReferenceDto> coreReferenceDtos = coreRefs.stream()
                .map(this::convertToReferenceDto)
                .collect(Collectors.toList());
        networkDto.setCoreReferences(coreReferenceDtos);

        return networkDto;
    }

    @Override
    public Map<String, Double> getPagerankScores(Long paperId) {
        List<Reference> references = referenceRepository.findByPaperId(paperId);
        Map<String, Double> scores = new HashMap<>();
        for (Reference ref : references) {
            scores.put(ref.getTitle(), ref.getPagerank() != null ? ref.getPagerank() : 0.0);
        }
        return scores;
    }

    @Override
    public List<Reference> getCoreReferences(Long paperId, int topK) {
        return referenceRepository.findTopKByPaperIdOrderByPagerankDesc(paperId, topK);
    }

    @Override
    @Transactional
    public CitationNetworkDto buildCitationNetwork(Long paperId, List<CitationDto> citations) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<Reference> existingRefs = referenceRepository.findByPaperId(paperId);
        List<Reference> savedRefs = new ArrayList<>();

        if (citations != null && !citations.isEmpty()) {
            int size = citations.size();
            for (int i = 0; i < size; i++) {
                CitationDto dto = citations.get(i);
                Reference ref = referenceRepository.findByTitle(dto.getTitle())
                        .orElseGet(() -> {
                            Reference newRef = new Reference();
                            newRef.setTitle(dto.getTitle());
                            newRef.setAuthors(dto.getAuthors());
                            newRef.setYear(dto.getYear());
                            newRef.setVenue(dto.getVenue());
                            return referenceRepository.save(newRef);
                        });

                double basePagerank = 1.0 / size;
                double positionBoost = (size - i) * 0.01;
                ref.setPagerank(basePagerank + positionBoost);
                referenceRepository.save(ref);
                savedRefs.add(ref);
            }
        }

        paper.setReferences(savedRefs);
        paperRepository.save(paper);

        return getCitationNetwork(paperId);
    }

    private CitationNetworkDto.ReferenceDto convertToReferenceDto(Reference ref) {
        CitationNetworkDto.ReferenceDto dto = new CitationNetworkDto.ReferenceDto();
        dto.setId(ref.getId());
        dto.setTitle(ref.getTitle());
        dto.setAuthors(ref.getAuthors());
        dto.setYear(ref.getYear());
        dto.setVenue(ref.getVenue());
        dto.setPagerank(ref.getPagerank());
        return dto;
    }
}
