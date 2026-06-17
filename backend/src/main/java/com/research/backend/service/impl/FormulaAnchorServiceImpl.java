package com.research.backend.service.impl;

import com.research.backend.dto.FormulaAnchorDto;
import com.research.backend.neo4j.entity.FormulaAnchor;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.repository.FormulaAnchorRepository;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.service.FormulaAnchorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaAnchorServiceImpl implements FormulaAnchorService {

    private final FormulaAnchorRepository formulaAnchorRepository;
    private final PaperRepository paperRepository;

    @Override
    public List<FormulaAnchorDto> getByPaperId(Long paperId) {
        List<FormulaAnchor> anchors = formulaAnchorRepository.findByPaperId(paperId);
        return anchors.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FormulaAnchorDto getByPaperIdAndNumber(Long paperId, String formulaNumber) {
        FormulaAnchor anchor = formulaAnchorRepository.findByPaperIdAndFormulaNumber(paperId, formulaNumber)
                .orElseThrow(() -> new RuntimeException("公式不存在"));
        return convertToDto(anchor);
    }

    @Override
    @Transactional
    public List<FormulaAnchorDto> saveFormulaAnchors(Long paperId, List<FormulaAnchorDto> anchorDtos) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<FormulaAnchor> existingAnchors = formulaAnchorRepository.findByPaperId(paperId);
        formulaAnchorRepository.deleteAll(existingAnchors);

        List<FormulaAnchor> savedAnchors = new ArrayList<>();
        for (FormulaAnchorDto dto : anchorDtos) {
            FormulaAnchor anchor = new FormulaAnchor();
            anchor.setFormulaNumber(dto.getFormulaNumber());
            anchor.setLatex(dto.getLatex());
            anchor.setPage(dto.getPage());
            anchor.setX0(dto.getX0());
            anchor.setY0(dto.getY0());
            anchor.setX1(dto.getX1());
            anchor.setY1(dto.getY1());
            anchor.setContext(dto.getContext());
            savedAnchors.add(formulaAnchorRepository.save(anchor));
        }

        paper.setFormulaAnchors(savedAnchors);
        paperRepository.save(paper);

        return savedAnchors.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormulaAnchorDto> searchByPattern(Long paperId, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return getByPaperId(paperId);
        }

        String cleanPattern = pattern.replaceAll("[^0-9.]+", "");
        List<FormulaAnchor> anchors = formulaAnchorRepository.findByPaperIdAndFormulaNumberContaining(paperId, cleanPattern);

        if (anchors.isEmpty()) {
            Pattern numPattern = Pattern.compile("(\\d+\\.?\\d*)");
            Matcher matcher = numPattern.matcher(pattern);
            if (matcher.find()) {
                String numPart = matcher.group(1);
                anchors = formulaAnchorRepository.findByPaperIdAndFormulaNumberContaining(paperId, numPart);
            }
        }

        return anchors.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private FormulaAnchorDto convertToDto(FormulaAnchor anchor) {
        FormulaAnchorDto dto = new FormulaAnchorDto();
        dto.setId(anchor.getId());
        dto.setFormulaNumber(anchor.getFormulaNumber());
        dto.setLatex(anchor.getLatex());
        dto.setPage(anchor.getPage());
        dto.setX0(anchor.getX0());
        dto.setY0(anchor.getY0());
        dto.setX1(anchor.getX1());
        dto.setY1(anchor.getY1());
        dto.setContext(anchor.getContext());
        return dto;
    }
}
