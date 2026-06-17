package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.ReviewerRecommendationDto;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.service.ReviewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewer")
@RequiredArgsConstructor
public class ReviewerController {

    private final ReviewerService reviewerService;

    @GetMapping("/recommend/{paperId}")
    public Result<List<ReviewerRecommendationDto>> recommendReviewers(@PathVariable Long paperId) {
        List<ReviewerRecommendationDto> recommendations = reviewerService.recommendReviewers(paperId);
        return Result.success(recommendations);
    }

    @GetMapping("/{id}")
    public Result<Reviewer> getReviewerById(@PathVariable Long id) {
        Reviewer reviewer = reviewerService.getReviewerById(id);
        return Result.success(reviewer);
    }

    @GetMapping("/list")
    public Result<List<Reviewer>> getAllReviewers() {
        List<Reviewer> reviewers = reviewerService.getAllReviewers();
        return Result.success(reviewers);
    }

    @PostMapping
    public Result<Reviewer> createReviewer(@RequestBody Reviewer reviewer) {
        Reviewer saved = reviewerService.saveReviewer(reviewer);
        return Result.success("创建成功", saved);
    }
}
