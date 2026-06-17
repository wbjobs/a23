package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.FormulaAnchorDto;
import com.research.backend.service.FormulaAnchorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formula")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaAnchorService formulaAnchorService;

    @GetMapping("/paper/{paperId}")
    public Result<List<FormulaAnchorDto>> getFormulasByPaperId(@PathVariable Long paperId) {
        List<FormulaAnchorDto> formulas = formulaAnchorService.getByPaperId(paperId);
        return Result.success(formulas);
    }

    @GetMapping("/paper/{paperId}/number/{number}")
    public Result<FormulaAnchorDto> getFormulaByNumber(@PathVariable Long paperId, @PathVariable String number) {
        FormulaAnchorDto formula = formulaAnchorService.getByPaperIdAndNumber(paperId, number);
        return Result.success(formula);
    }

    @GetMapping("/paper/{paperId}/search")
    public Result<List<FormulaAnchorDto>> searchFormulas(@PathVariable Long paperId, @RequestParam String pattern) {
        List<FormulaAnchorDto> formulas = formulaAnchorService.searchByPattern(paperId, pattern);
        return Result.success(formulas);
    }
}
