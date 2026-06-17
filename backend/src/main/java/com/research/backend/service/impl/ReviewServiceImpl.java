package com.research.backend.service.impl;

import com.research.backend.dto.ReviewAssignDto;
import com.research.backend.dto.ReviewSubmitRequest;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.entity.ReviewRecord;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.repository.ReviewRecordRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.service.EmailService;
import com.research.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewerRepository reviewerRepository;
    private final PaperRepository paperRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public List<ReviewRecord> assignReviewers(ReviewAssignDto assignDto) {
        Paper paper = paperRepository.findById(assignDto.getPaperId())
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<ReviewRecord> records = new ArrayList<>();
        for (Long reviewerId : assignDto.getReviewerIds()) {
            Reviewer reviewer = reviewerRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("审稿人不存在"));

            if (Boolean.TRUE.equals(reviewRecordRepository.existsByPaperIdAndReviewerId(assignDto.getPaperId(), reviewerId))) {
                continue;
            }

            ReviewRecord record = ReviewRecord.builder()
                    .paperId(assignDto.getPaperId())
                    .reviewerId(reviewerId)
                    .reviewerName(reviewer.getName())
                    .status(ReviewRecord.ReviewStatus.ASSIGNED)
                    .assignTime(LocalDateTime.now())
                    .build();
            ReviewRecord saved = reviewRecordRepository.save(record);
            records.add(saved);

            try {
                emailService.sendReviewNotification(reviewer, paper);
            } catch (Exception ignored) {
            }
        }

        if (!records.isEmpty()) {
            paper.setStatus("UNDER_REVIEW");
            paper.setUpdateTime(LocalDateTime.now());
            paperRepository.save(paper);
        }

        return records;
    }

    @Override
    @Transactional
    public ReviewRecord submitReview(ReviewSubmitRequest submitRequest) {
        ReviewRecord record = reviewRecordRepository
                .findByPaperIdAndReviewerId(submitRequest.getPaperId(), submitRequest.getReviewerId())
                .orElseGet(() -> ReviewRecord.builder()
                        .paperId(submitRequest.getPaperId())
                        .reviewerId(submitRequest.getReviewerId())
                        .build());

        record.setInnovationScore(submitRequest.getInnovationScore());
        record.setMethodScore(submitRequest.getMethodScore());
        record.setWritingScore(submitRequest.getWritingScore());
        record.setOverallScore(submitRequest.getOverallScore());
        record.setComments(submitRequest.getComments());
        record.setStatus(ReviewRecord.ReviewStatus.SUBMITTED);
        record.setReviewTime(LocalDateTime.now());

        ReviewRecord saved = reviewRecordRepository.save(record);

        checkAndUpdatePaperStatus(submitRequest.getPaperId());

        return saved;
    }

    private void checkAndUpdatePaperStatus(Long paperId) {
        List<ReviewRecord> records = reviewRecordRepository.findByPaperId(paperId);
        if (records.isEmpty()) return;

        boolean allCompleted = records.stream()
                .allMatch(r -> ReviewRecord.ReviewStatus.SUBMITTED.equals(r.getStatus())
                        || ReviewRecord.ReviewStatus.ACCEPTED.equals(r.getStatus())
                        || ReviewRecord.ReviewStatus.REJECTED.equals(r.getStatus()));
        if (allCompleted) {
            Paper paper = paperRepository.findById(paperId).orElse(null);
            if (paper != null) {
                double avgScore = records.stream()
                        .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                        .average().orElse(0);

                boolean allAccept = records.stream()
                        .allMatch(r -> r.getOverallScore() != null && r.getOverallScore() >= 60);

                String result = avgScore >= 70 && allAccept ? "ACCEPTED" :
                        avgScore >= 50 ? "REVISION_REQUIRED" : "REJECTED";

                paper.setStatus(result);
                paper.setUpdateTime(LocalDateTime.now());
                paperRepository.save(paper);
            }
        }
    }

    @Override
    public List<ReviewRecord> getReviewsByPaper(Long paperId) {
        return reviewRecordRepository.findByPaperId(paperId);
    }

    @Override
    public List<ReviewRecord> getReviewsByReviewer(Long reviewerId) {
        return reviewRecordRepository.findByReviewerId(reviewerId);
    }
}
