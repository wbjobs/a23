package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Paper;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends Neo4jRepository<Paper, Long> {
    List<Paper> findBySubmitterId(Long submitterId);
    List<Paper> findByStatus(String status);
    Optional<Paper> findByTitle(String title);

    @Query("MATCH (p:Paper)-[:AUTHORED_BY]->(a:Author) WHERE id(a) = $authorId RETURN p")
    List<Paper> findPapersByAuthorId(Long authorId);

    @Query("MATCH (p:Paper)-[:AUTHORED_BY]->(a:Author) WHERE a.name = $authorName RETURN p")
    List<Paper> findPapersByAuthorName(String authorName);

    @Query("MATCH (p1:Paper)-[:HAS_KEYWORD]->(k:Keyword)<-[:HAS_KEYWORD]-(p2:Paper) WHERE id(p1) = $paperId AND id(p2) <> id(p1) RETURN p2, count(k) as commonCount ORDER BY commonCount DESC LIMIT 10")
    List<Paper> findSimilarPapers(Long paperId);
}
