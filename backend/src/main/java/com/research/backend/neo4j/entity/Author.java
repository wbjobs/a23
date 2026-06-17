package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import java.util.List;

@Data
@Node("Author")
public class Author {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String email;
    private String orcid;

    @Relationship(type = "AFFILIATED_WITH", direction = Relationship.Direction.OUTGOING)
    private Institution institution;
}
