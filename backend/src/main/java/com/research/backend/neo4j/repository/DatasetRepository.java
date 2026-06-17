package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Dataset;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetRepository extends Neo4jRepository<Dataset, Long> {
    Optional<Dataset> findByName(String name);
    boolean existsByName(String name);
}
