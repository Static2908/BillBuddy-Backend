
package com.example.backend.rag;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class KnowledgeBaseLoader {

    private static final String RESOURCE_PATTERN = "classpath:/rag/*.txt";

    private static final String CHUNK_SEPARATOR =
            "==================================================";

    /*
     * Heading-only chunks such as "6. WATER BILL ANOMALIES"
     * should not be returned as useful RAG evidence.
     */
    private static final Pattern SECTION_HEADING = Pattern.compile(
            "(?im)^\\s*(?:\\d+\\.\\s*)?[A-Z][A-Z0-9 /&(),-]{2,}\\s*$"
    );

    private final List<RagChunk> chunks;

    public KnowledgeBaseLoader() {
        this.chunks = Collections.unmodifiableList(loadChunks());
    }

    private List<RagChunk> loadChunks() {
        List<RagChunk> loadedChunks = new ArrayList<>();

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources =
                    resolver.getResources(RESOURCE_PATTERN);

            for (Resource resource : resources) {
                String filename = resource.getFilename();

                if (filename == null) {
                    continue;
                }

                String content;

                try (var inputStream = resource.getInputStream()) {
                    content = new String(
                            inputStream.readAllBytes(),
                            StandardCharsets.UTF_8
                    );
                }

                String[] sections = content.split(
                        "(?m)^\\s*" + Pattern.quote(CHUNK_SEPARATOR)
                                + "\\s*$"
                );

                for (String section : sections) {
                    String cleaned = cleanSection(section);

                    if (isUsefulChunk(cleaned)) {
                        loadedChunks.add(
                                new RagChunk(filename, cleaned)
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load RAG knowledge-base files from "
                            + RESOURCE_PATTERN,
                    e
            );
        }

        if (loadedChunks.isEmpty()) {
            throw new IllegalStateException(
                    "No useful RAG chunks were loaded from "
                            + RESOURCE_PATTERN
            );
        }

        return loadedChunks;
    }

    private String cleanSection(String section) {
        if (section == null) {
            return "";
        }

        return section
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((first, second) -> first + "\n" + second)
                .orElse("")
                .strip();
    }

    private boolean isUsefulChunk(String content) {
        if (content.isBlank()) {
            return false;
        }

        // Reject a section containing only a heading.
        if (SECTION_HEADING.matcher(content).matches()) {
            return false;
        }

        // Reject chunks that contain no meaningful descriptive text.
        long wordCount = content
                .split("\\s+")
                .length;

        return wordCount >= 8;
    }

    public List<RagChunk> getChunks() {
        return chunks;
    }
}
