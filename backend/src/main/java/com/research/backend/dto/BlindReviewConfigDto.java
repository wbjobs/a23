package com.research.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class BlindReviewConfigDto {

    private boolean enabled;

    private boolean autoDesensitize;

    private List<String> desensitizeFields;
}
