package com.research.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableNeo4jRepositories(basePackages = "com.research.backend.neo4j.repository")
@EnableJpaRepositories(basePackages = "com.research.backend.repository")
@EntityScan(basePackages = {"com.research.backend.entity", "com.research.backend.neo4j.entity"})
public class ResearchBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResearchBackendApplication.class, args);
    }
}
