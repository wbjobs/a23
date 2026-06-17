package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.FormulaAnchor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaAnchorRepository extends Neo4jRepository<FormulaAnchor, Long> {

    @Query("MATCH (p:Paper)-[:HAS_FORMULA]->(f:Formula) WHERE id(p) = $paperId RETURN f")
    List<FormulaAnchor> findByPaperId(Long paperId);

    @Query("MATCH (p:Paper)-[:HAS_FORMULA]->(f:Formula) WHERE id(p) = $paperId AND f.formulaNumber = $formulaNumber RETURN f")
    Optional<FormulaAnchor> findByPaperIdAndFormulaNumber(Long paperId, String formulaNumber);

    @Query("MATCH (p:Paper)-[:HAS_FORMULA]->(f:Formula) WHERE id(p) = $paperId AND f.formulaNumber CONTAINS $pattern RETURN f")
    List<FormulaAnchor> findByPaperIdAndFormulaNumberContaining(Long paperId, String pattern);
}
