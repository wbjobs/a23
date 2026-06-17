package com.research.backend.repository;

import com.research.backend.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
