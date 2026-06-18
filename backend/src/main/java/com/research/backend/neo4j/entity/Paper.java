package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Node("Paper")
public class Paper {
    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String abstractText;
    private String filePath;
    private String authorName;
    private Long submitterId;
    private String status;
    private LocalDateTime uploadTime;
    private LocalDateTime updateTime;
    private String innovation;
    private List<String> methods;

    private Boolean blindReviewEnabled;

    private String desensitizedTitle;

    @Relationship(type = "AUTHORED_BY", direction = Relationship.Direction.OUTGOING)
    private List<Author> authors;

    @Relationship(type = "HAS_KEYWORD", direction = Relationship.Direction.OUTGOING)
    private List<Keyword> keywords;

    @Relationship(type = "USES_DATASET", direction = Relationship.Direction.OUTGOING)
    private List<Dataset> datasets;

    @Relationship(type = "HAS_FORMULA", direction = Relationship.Direction.OUTGOING)
    private List<FormulaAnchor> formulaAnchors;

    @Relationship(type = "HAS_TOPIC", direction = Relationship.Direction.OUTGOING)
    private List<Topic> topics;

    @Relationship(type = "CITES", direction = Relationship.Direction.OUTGOING)
    private List<Reference> references;
}
