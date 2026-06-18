package com.research.backend.neo4j.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.List;

@RelationshipProperties
@Data
public class SimilarityEdge {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Paper targetPaper;

    private Double similarityScore;
    private String similarityType;
    private List<String> overlappingDatasets;
    private List<String> overlappingKeywords;
    private List<String> overlappingReferences;
    private Double overlapRatio;
    private String riskLevel;
    private LocalDateTime checkTime;
}
