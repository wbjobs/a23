package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.ReviewAssignDto;
import com.research.backend.dto.ReviewSubmitRequest;
import com.research.backend.entity.ReviewRecord;
import com.research.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Result<List<ReviewRecord>> assignReviewers(@Valid @RequestBody ReviewAssignDto assignDto) {
        List<ReviewRecord> records = reviewService.assignReviewers(assignDto);
        return Result.success("分配成功", records);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<ReviewRecord> submitReview(@Valid @RequestBody ReviewSubmitRequest submitRequest) {
        ReviewRecord record = reviewService.submitReview(submitRequest);
        return Result.success("提交成功", record);
    }

    @GetMapping("/paper/{paperId}")
    public Result<List<ReviewRecord>> getReviewsByPaper(@PathVariable Long paperId) {
        List<ReviewRecord> records = reviewService.getReviewsByPaper(paperId);
        return Result.success(records);
    }

    @GetMapping("/reviewer/{reviewerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<List<ReviewRecord>> getReviewsByReviewer(@PathVariable Long reviewerId) {
        List<ReviewRecord> records = reviewService.getReviewsByReviewer(reviewerId);
        return Result.success(records);
    }
}
