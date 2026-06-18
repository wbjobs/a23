package com.research.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_quality_metrics")
public class ReviewQualityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(nullable = false)
    private Integer period;

    @Column(name = "calculate_time", nullable = false)
    private LocalDateTime calculateTime;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "average_overall_score")
    private Double averageOverallScore;

    @Column(name = "average_innovation_score")
    private Double averageInnovationScore;

    @Column(name = "average_method_score")
    private Double averageMethodScore;

    @Column(name = "average_writing_score")
    private Double averageWritingScore;

    @Column(name = "average_review_duration_hours")
    private Double averageReviewDurationHours;

    @Column(name = "median_review_duration_hours")
    private Double medianReviewDurationHours;

    @Column(name = "score_standard_deviation")
    private Double scoreStandardDeviation;

    @Column(name = "score_coefficient_of_variation")
    private Double scoreCoefficientOfVariation;

    @Column(name = "timely_completed_count")
    private Integer timelyCompletedCount;

    @Column(name = "over_due_count")
    private Integer overDueCount;

    @Column(name = "timely_completion_rate")
    private Double timelyCompletionRate;

    @Column(name = "rejected_by_author_count")
    private Integer rejectedByAuthorCount;

    @Column(name = "accepted_by_author_count")
    private Integer acceptedByAuthorCount;

    @Column(name = "comment_word_count_average")
    private Double commentWordCountAverage;

    @Column(name = "comment_detail_score")
    private Double commentDetailScore;

    @Column(name = "overall_quality_score")
    private Double overallQualityScore;

    @Column(name = "quality_level", length = 20)
    private String qualityLevel;
}
