package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Formula")
public class FormulaAnchor {
    @Id
    @GeneratedValue
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
