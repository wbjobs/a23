package com.research.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_reply")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_record_id", nullable = false)
    private Long reviewRecordId;

    @Column(name = "paper_id")
    private Long paperId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "thread_number")
    private Integer threadNumber;

    @Column(name = "original_comment_segment", columnDefinition = "TEXT")
    private String originalCommentSegment;

    @Column(name = "parent_reply_id")
    private Long parentReplyId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "suggested_references_json", columnDefinition = "JSON")
    private String suggestedReferencesJson;

    @Column(name = "cited_literature_json", columnDefinition = "JSON")
    private String citedLiteratureJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ReplyStatus status = ReplyStatus.DRAFT;

    public enum ReplyStatus {
        DRAFT, SUBMITTED, RESOLVED
    }

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    private Boolean resolved;

    @Column(name = "reviewer_response", columnDefinition = "TEXT")
    private String reviewerResponse;

    @Column(name = "reviewer_response_time")
    private LocalDateTime reviewerResponseTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (resolved == null) {
            resolved = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
