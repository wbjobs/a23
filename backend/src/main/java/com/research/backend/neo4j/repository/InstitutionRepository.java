package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Institution;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends Neo4jRepository<Institution, Long> {
    Optional<Institution> findByName(String name);
}
