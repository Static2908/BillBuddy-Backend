package com.example.backend.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OllamaService {

    private static final Logger log =
            LoggerFactory.getLogger(OllamaService.class);

    private final RestClient restClient;

    private final String model;

    public OllamaService(
            @Value("${app.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ollama.model:llama3.2:3b}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
    }

    public String generate(String prompt) {
        long startTime = System.nanoTime();

        int promptLength = prompt == null ? 0 : prompt.length();

        log.info(
                "Ollama generation started. model={}, promptChars={}",
                model,
                promptLength
        );

        try {
            Map<String, Object> request = Map.of(
        "model", model,
        "prompt", prompt,
        "stream", false,
        "keep_alive", "10m",
        "options", Map.of(
                "num_predict", 120,
                "temperature", 0.2,
                "num_ctx", 4096
        )
);

            Map<?, ?> response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("response") == null) {
                throw new IllegalStateException(
                        "Ollama returned an empty response"
                );
            }

            String generatedText = response.get("response").toString();

            long elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.info(
                    "Ollama generation completed. durationMs={}, " +
                            "promptChars={}, responseChars={}",
                    elapsedMs,
                    promptLength,
                    generatedText.length()
            );

            return generatedText;

        } catch (RuntimeException exception) {
            long elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.error(
                    "Ollama generation failed. durationMs={}, " +
                            "promptChars={}, error={}",
                    elapsedMs,
                    promptLength,
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }
}