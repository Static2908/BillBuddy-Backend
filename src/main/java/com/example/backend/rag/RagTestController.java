package com.example.backend.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Profile("dev")
@RestController
@RequestMapping("/api/rag")
public class RagTestController {

    private final RagRetriever ragRetriever;

    public RagTestController(RagRetriever ragRetriever) {
        this.ragRetriever = ragRetriever;
    }

    @GetMapping("/search-context")
    public List<RagChunk> searchWithContext(
            @RequestParam String query,
            @RequestParam String utilityType,
            @RequestParam String anomalies
    ) {

        List<String> anomalyRules =
                Arrays.stream(anomalies.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();

        return ragRetriever.retrieve(
                query,
                utilityType,
                anomalyRules,
                3
        );
    }
}