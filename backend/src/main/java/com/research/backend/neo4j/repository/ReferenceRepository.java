package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Reference;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceRepository extends Neo4jRepository<Reference, Long> {

    Optional<Reference> findByTitle(String title);

    List<Reference> findByTitleContaining(String keyword);

    List<Reference> findTop10ByOrderByPagerankDesc();

    @Query("MATCH (p:Paper)-[:CITES]->(r:Reference) WHERE id(p) = $paperId RETURN r")
    List<Reference> findByPaperId(Long paperId);

    @Query("MATCH (p:Paper)-[:CITES]->(r:Reference) WHERE id(p) = $paperId RETURN r ORDER BY r.pagerank DESC LIMIT $topK")
    List<Reference> findTopKByPaperIdOrderByPagerankDesc(Long paperId, int topK);
}
