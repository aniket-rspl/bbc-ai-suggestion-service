package com.jaypharma.aiservice.config;

import com.jaypharma.aiservice.service.preprocessing.InMemoryNormalizationKnowledgeStore;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NormalizationProperties.class)
public class NormalizationConfig {

    @Bean
    public NormalizationKnowledgeStore normalizationKnowledgeStore(NormalizationProperties properties) {
        return new InMemoryNormalizationKnowledgeStore(properties);
    }
}
