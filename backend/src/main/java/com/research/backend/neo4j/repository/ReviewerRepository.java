package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Reviewer;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerRepository extends Neo4jRepository<Reviewer, Long> {
    Optional<Reviewer> findByEmail(String email);
    List<Reviewer> findByNameContaining(String name);

    @Query("MATCH (r:Reviewer), (p:Paper) WHERE id(p) = $paperId RETURN r ORDER BY gds.similarity.cosine(r.vector, p.vector) DESC LIMIT 10")
    List<Reviewer> findReviewersByCosineSimilarity(Long paperId);
}
