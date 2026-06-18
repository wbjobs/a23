package com.research.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_record")
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id", nullable = false)
    private Long paperId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(name = "innovation_score")
    private Integer innovationScore;

    @Column(name = "method_score")
    private Integer methodScore;

    @Column(name = "writing_score")
    private Integer writingScore;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "assign_time")
    private LocalDateTime assignTime;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    private Boolean anonymous;

    private String reviewerAlias;

    private Boolean authorInfoDesensitized;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Transient
    private Long reviewDurationMinutes;

    public Long getReviewDurationMinutes() {
        if (assignTime != null && reviewTime != null) {
            return Duration.between(assignTime, reviewTime).toMinutes();
        }
        return null;
    }

    public enum ReviewStatus {
        PENDING,
        ASSIGNED,
        SUBMITTED,
        ACCEPTED,
        REJECTED
    }
}
