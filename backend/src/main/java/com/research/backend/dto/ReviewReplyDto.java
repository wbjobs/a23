package com.research.backend.dto;

import com.research.backend.entity.ReviewReply;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyDto {

    private Long id;

    private Long reviewRecordId;

    private Integer threadNumber;

    private Long parentReplyId;

    private String content;

    private List<ReferenceDto> suggestedReferences;

    private List<ReferenceDto> citedLiterature;

    private String authorName;

    private LocalDateTime createTime;

    private ReviewReply.ReplyStatus status;

    private Boolean resolved;

    private String reviewerResponse;

    private LocalDateTime reviewerResponseTime;
}
