package com.research.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class DuplicateCheckRequest {
    private Long paperId;
    private List<String> checkTypes;
}
