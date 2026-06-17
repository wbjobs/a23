package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Keyword")
public class Keyword {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
