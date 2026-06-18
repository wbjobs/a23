package com.research.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "duplicate_check_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paperId;

    private String paperTitle;

    private LocalDateTime checkTime;

    private String overallRiskLevel;

    private Double overallSimilarity;

    @Column(columnDefinition = "JSON")
    private String similarPapersJson;

    @Column(columnDefinition = "JSON")
    private String sausageDetectionJson;

    @Column(columnDefinition = "JSON")
    private String evidenceDetailsJson;

    private String checker;

    private String checkNote;

    private String reportStatus;

    private Integer suspectedSausageCount;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
