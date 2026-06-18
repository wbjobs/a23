package com.research.backend.repository;

import com.research.backend.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    List<ReviewReply> findByReviewRecordIdOrderByThreadNumberAscCreateTimeAsc(Long reviewRecordId);

    List<ReviewReply> findByPaperIdAndAuthorIdOrderByCreateTimeDesc(Long paperId, Long authorId);

    List<ReviewReply> findByReviewRecordIdAndThreadNumberOrderByCreateTimeAsc(Long reviewRecordId, Integer threadNumber);

    Long countByReviewRecordIdAndResolvedFalse(Long reviewRecordId);
}
