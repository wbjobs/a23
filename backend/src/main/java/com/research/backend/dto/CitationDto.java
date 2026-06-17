package com.research.backend.dto;

import lombok.Data;

@Data
public class CitationDto {
    private String title;
    private String authors;
    private Integer year;
    private String venue;
}
