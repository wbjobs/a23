package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyThreadDto {

    private Integer threadNumber;

    private String originalComment;

    private List<ReviewReplyDto> replies;

    private Boolean resolved;
}
