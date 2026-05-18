package com.jaypharma.aiservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiConfigLogger implements CommandLineRunner {

    @Value("${spring.ai.ollama.base-url:NOT_FOUND}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:NOT_FOUND}")
    private String ollamaModel;

    @Override
    public void run(String... args) {
        log.info("Resolved Ollama base URL: {}", ollamaBaseUrl);
        log.info("Resolved Ollama model: {}", ollamaModel);
    }
}