package com.research.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCalculateRequest {

    private List<Long> reviewerIds;

    @NotNull(message = "统计周期不能为空")
    private Integer period;
}
