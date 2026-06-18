package com.research.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.blind-review")
@Data
public class BlindReviewConfig {

    private boolean enabledDefault = true;

    private boolean autoDesensitize = true;

    private List<String> desensitizeFields = Arrays.asList("authors", "affiliation", "acknowledgements");

    private String reviewerAliasPattern = "评审专家%s";
}
