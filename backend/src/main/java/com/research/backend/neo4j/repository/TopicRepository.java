package com.research.backend.neo4j.repository;

import com.research.backend.neo4j.entity.Topic;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends Neo4jRepository<Topic, Long> {

    @Query("MATCH (p:Paper)-[:HAS_TOPIC]->(t:Topic) WHERE id(p) = $paperId AND t.isCore = true RETURN t")
    List<Topic> findCoreTopicsByPaperId(Long paperId);

    @Query("MATCH (p:Paper)-[:HAS_TOPIC]->(t:Topic) WHERE id(p) = $paperId RETURN t ORDER BY t.weight DESC LIMIT 10")
    List<Topic> findTop10ByPaperIdOrderByWeightDesc(Long paperId);

    @Query("MATCH (p:Paper)-[:HAS_TOPIC]->(t:Topic) WHERE id(p) = $paperId RETURN t")
    List<Topic> findByPaperId(Long paperId);

    List<Topic> findByIsCoreTrue();

    List<Topic> findTop10ByOrderByWeightDesc();
}
