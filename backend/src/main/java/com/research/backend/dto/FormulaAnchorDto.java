package com.research.backend.dto;

import lombok.Data;

@Data
public class FormulaAnchorDto {
    private Long id;
    private String formulaNumber;
    private String latex;
    private Integer page;
    private Double x0;
    private Double y0;
    private Double x1;
    private Double y1;
    private String context;
}
