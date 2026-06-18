package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDto {

    private Long id;

    private String title;

    private String authors;

    private Integer year;

    private String venue;

    private Double similarityScore;

    private String abstractSnippet;
}
