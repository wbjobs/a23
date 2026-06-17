package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Reference")
public class Reference {
    @Id
    @GeneratedValue
    private Long id;

    private String title;

    private String authors;

    private Integer year;

    private String venue;

    private Double pagerank;
}
