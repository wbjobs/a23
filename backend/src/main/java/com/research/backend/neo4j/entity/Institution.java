package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Institution")
public class Institution {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String address;
    private String country;
}
