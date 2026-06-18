package com.research.backend.service;

import com.research.backend.dto.QualityTrendDto;
import com.research.backend.dto.ReviewQualityDashboardDto;
import com.research.backend.dto.ReviewerDetailQualityDto;
import com.research.backend.dto.ReviewerRankingDto;
import com.research.backend.entity.ReviewQualityMetrics;

import java.util.List;
import java.util.Map;

public interface ReviewQualityService {

    ReviewQualityDashboardDto getDashboardData();

    ReviewerDetailQualityDto getReviewerQuality(Long reviewerId);

    ReviewQualityMetrics calculateMetrics(Long reviewerId, Integer periodMonths);

    List<ReviewerRankingDto> getReviewerRanking(String sortBy, Integer limit);

    Map<String, Object> getScoreDistribution(Integer periodMonths);

    Map<String, Object> getDurationDistribution(Integer periodMonths);

    List<QualityTrendDto> getTrendData(Integer months);

    void batchCalculate(List<Long> reviewerIds, Integer periodMonths);
}
