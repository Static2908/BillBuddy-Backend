package com.example.backend.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/ollama")
public class OllamaTestController {

    private final OllamaService ollamaService;

    public OllamaTestController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/test")
    public Map<String, String> test(
            @RequestParam(defaultValue =
                    "Explain in one sentence why an electricity bill can increase.")
            String prompt) {

        String response = ollamaService.generate(prompt);

        return Map.of("response", response);
    }
}
