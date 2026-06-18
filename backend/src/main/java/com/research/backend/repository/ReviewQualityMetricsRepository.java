package com.research.backend.repository;

import com.research.backend.entity.ReviewQualityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewQualityMetricsRepository extends JpaRepository<ReviewQualityMetrics, Long> {

    List<ReviewQualityMetrics> findByReviewerIdOrderByCalculateTimeDesc(Long reviewerId);

    List<ReviewQualityMetrics> findByPeriodOrderByOverallQualityScoreDesc(Integer period);

    Optional<ReviewQualityMetrics> findTopByReviewerIdOrderByCalculateTimeDesc(Long reviewerId);
}
