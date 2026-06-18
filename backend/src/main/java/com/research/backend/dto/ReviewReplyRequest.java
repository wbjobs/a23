package com.research.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewReplyRequest {

    @NotNull
    private Long reviewRecordId;

    private Integer threadNumber;

    private Long parentReplyId;

    @NotBlank
    private String content;

    private List<String> citedLiterature;
}
