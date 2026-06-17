package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Dataset")
public class Dataset {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String description;
    private String source;
}
