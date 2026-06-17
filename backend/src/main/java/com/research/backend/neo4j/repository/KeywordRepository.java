package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Keyword;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends Neo4jRepository<Keyword, Long> {
    Optional<Keyword> findByName(String name);
    boolean existsByName(String name);

    @Query("MATCH (p:Paper)-[:HAS_KEYWORD]->(k:Keyword) WHERE id(p) = $paperId RETURN k")
    List<Keyword> findKeywordsByPaperId(Long paperId);

    @Query("MATCH (r:Reviewer)-[:EXPERT_IN]->(k:Keyword) WHERE id(r) = $reviewerId RETURN k")
    List<Keyword> findKeywordsByReviewerId(Long reviewerId);
}
