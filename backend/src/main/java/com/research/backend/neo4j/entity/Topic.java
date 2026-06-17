package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Topic")
public class Topic {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Double weight;

    private Boolean isCore;

    private String source;
}
