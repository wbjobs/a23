package com.research.backend.service;

import com.research.backend.dto.ReviewAssignDto;
import com.research.backend.dto.ReviewSubmitRequest;
import com.research.backend.entity.ReviewRecord;

import java.util.List;

public interface ReviewService {
    List<ReviewRecord> assignReviewers(ReviewAssignDto assignDto);
    ReviewRecord submitReview(ReviewSubmitRequest submitRequest);
    List<ReviewRecord> getReviewsByPaper(Long paperId);
    List<ReviewRecord> getReviewsByReviewer(Long reviewerId);
}
