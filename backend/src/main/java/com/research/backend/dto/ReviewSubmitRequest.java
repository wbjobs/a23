package com.research.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitRequest {

    @NotNull(message = "论文ID不能为空")
    private Long paperId;

    @NotNull(message = "审稿人ID不能为空")
    private Long reviewerId;

    @NotNull(message = "创新评分不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer innovationScore;

    @NotNull(message = "方法评分不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer methodScore;

    @NotNull(message = "写作评分不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer writingScore;

    @NotNull(message = "综合评分不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer overallScore;

    private String comments;
}
