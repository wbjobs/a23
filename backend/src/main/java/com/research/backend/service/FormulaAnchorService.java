package com.research.backend.service;

import com.research.backend.dto.FormulaAnchorDto;

import java.util.List;

public interface FormulaAnchorService {
    List<FormulaAnchorDto> getByPaperId(Long paperId);
    FormulaAnchorDto getByPaperIdAndNumber(Long paperId, String formulaNumber);
    List<FormulaAnchorDto> saveFormulaAnchors(Long paperId, List<FormulaAnchorDto> anchors);
    List<FormulaAnchorDto> searchByPattern(Long paperId, String pattern);
}
