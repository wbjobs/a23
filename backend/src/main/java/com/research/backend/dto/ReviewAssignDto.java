package com.research.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAssignDto {

    @NotNull(message = "论文ID不能为空")
    private Long paperId;

    @NotNull(message = "审稿人ID列表不能为空")
    private List<Long> reviewerIds;
}
