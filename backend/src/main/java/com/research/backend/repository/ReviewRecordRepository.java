package com.research.backend.repository;

import com.research.backend.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findByPaperId(Long paperId);

    List<ReviewRecord> findByReviewerId(Long reviewerId);

    Optional<ReviewRecord> findByPaperIdAndReviewerId(Long paperId, Long reviewerId);

    Boolean existsByPaperIdAndReviewerId(Long paperId, Long reviewerId);

    List<ReviewRecord> findByPaperIdAndStatus(Long paperId, ReviewRecord.ReviewStatus status);

    List<ReviewRecord> findByReviewerIdAndStatus(Long reviewerId, ReviewRecord.ReviewStatus status);

    @Query("SELECT r FROM ReviewRecord r WHERE r.reviewerId = :reviewerId AND r.status = :status AND r.reviewTime >= :startTime")
    List<ReviewRecord> findByReviewerIdAndStatusAndReviewTimeAfter(
            @Param("reviewerId") Long reviewerId,
            @Param("status") ReviewRecord.ReviewStatus status,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT r FROM ReviewRecord r WHERE r.status = :status AND r.reviewTime >= :startTime")
    List<ReviewRecord> findByStatusAndReviewTimeAfter(
            @Param("status") ReviewRecord.ReviewStatus status,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(DISTINCT r.reviewerId) FROM ReviewRecord r WHERE r.status = 'SUBMITTED'")
    Long countDistinctReviewersWithSubmittedReviews();

    @Query("SELECT COUNT(DISTINCT r.paperId) FROM ReviewRecord r WHERE r.status = 'SUBMITTED'")
    Long countDistinctPapersWithSubmittedReviews();

    @Query("SELECT r FROM ReviewRecord r WHERE r.paperId = :paperId AND r.reviewerId != :excludeReviewerId AND r.status = 'SUBMITTED'")
    List<ReviewRecord> findByPaperIdAndReviewerIdNotAndStatus(
            @Param("paperId") Long paperId,
            @Param("excludeReviewerId") Long excludeReviewerId,
            @Param("status") ReviewRecord.ReviewStatus status);
}
