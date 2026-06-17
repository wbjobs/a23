package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import java.util.List;

@Data
@Node("Reviewer")
public class Reviewer {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String email;
    private String affiliation;
    private String title;
    private String researchAreas;

    @Relationship(type = "EXPERT_IN", direction = Relationship.Direction.OUTGOING)
    private List<Keyword> expertKeywords;
}
