package com.research.backend.service;

import com.research.backend.dto.ReviewerRecommendationDto;
import com.research.backend.neo4j.entity.Reviewer;

import java.util.List;

public interface ReviewerService {
    List<ReviewerRecommendationDto> recommendReviewers(Long paperId);
    Reviewer getReviewerById(Long id);
    List<Reviewer> getAllReviewers();
    Reviewer saveReviewer(Reviewer reviewer);
}
