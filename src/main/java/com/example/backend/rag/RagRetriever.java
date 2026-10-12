
package com.example.backend.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagRetriever {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[a-zA-Z0-9_]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been",
            "by", "can", "compared", "for", "from", "has", "have",
            "how", "in", "is", "it", "of", "on", "or", "should",
            "that", "the", "their", "this", "to", "was", "were",
            "what", "when", "which", "with"
    );

    private final KnowledgeBaseLoader knowledgeBaseLoader;

    public RagRetriever(KnowledgeBaseLoader knowledgeBaseLoader) {
        this.knowledgeBaseLoader = knowledgeBaseLoader;
    }

    /**
     * Retrieves reference chunks for a query, utility type, and
     * the anomaly rules detected by Java.
     *
     * Chunks belonging to a different utility are excluded.
     * General billing and anomaly-reference chunks remain eligible.
     */
    public List<RagChunk> retrieve(
            String query,
            String utilityType,
            List<String> anomalyRules,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }

        String normalizedUtility = normalizeUtility(utilityType);
        Set<String> queryTokens = tokenize(query);

        List<String> rules = anomalyRules == null
                ? List.of()
                : anomalyRules.stream()
                        .filter(rule -> rule != null && !rule.isBlank())
                        .map(rule -> rule.toUpperCase(Locale.ROOT))
                        .distinct()
                        .toList();

        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (RagChunk chunk : knowledgeBaseLoader.getChunks()) {
            if (chunk == null || chunk.content() == null) {
                continue;
            }

            String source = chunk.source() == null
                    ? ""
                    : chunk.source().toLowerCase(Locale.ROOT);

            // Do not allow a utility-specific file for another utility.
            if (isWrongUtilitySource(source, normalizedUtility)) {
                continue;
            }

            int score = calculateScore(
                    chunk,
                    source,
                    queryTokens,
                    normalizedUtility,
                    rules
            );

            // Ignore chunks with no relevance to the query or utility.
            if (score > 0) {
                scoredChunks.add(new ScoredChunk(chunk, score));
            }
        }

        return scoredChunks.stream()
                .sorted(
                        Comparator.comparingInt(ScoredChunk::score)
                                .reversed()
                                .thenComparing(
                                        item -> item.chunk().source(),
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                )
                .limit(limit)
                .map(ScoredChunk::chunk)
                .toList();
    }

    /**
     * Compatibility overload for callers that only provide a query
     * and limit. It performs general keyword retrieval without a
     * utility-specific filter.
     */
    public List<RagChunk> retrieve(String query, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        Set<String> queryTokens = tokenize(query);
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (RagChunk chunk : knowledgeBaseLoader.getChunks()) {
            if (chunk == null || chunk.content() == null) {
                continue;
            }

            String source = chunk.source() == null
                    ? ""
                    : chunk.source().toLowerCase(Locale.ROOT);

            int score = calculateScore(
                    chunk,
                    source,
                    queryTokens,
                    null,
                    List.of()
            );

            if (score > 0) {
                scoredChunks.add(new ScoredChunk(chunk, score));
            }
        }

        return scoredChunks.stream()
                .sorted(
                        Comparator.comparingInt(ScoredChunk::score)
                                .reversed()
                                .thenComparing(
                                        item -> item.chunk().source(),
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                )
                .limit(limit)
                .map(ScoredChunk::chunk)
                .toList();
    }

    private int calculateScore(
            RagChunk chunk,
            String normalizedSource,
            Set<String> queryTokens,
            String utilityType,
            List<String> anomalyRules
    ) {
        String content = chunk.content().toLowerCase(Locale.ROOT);
        int score = 0;

        // Score overlap between query words and chunk content.
        Set<String> contentTokens = tokenize(content);

        for (String token : queryTokens) {
            if (contentTokens.contains(token)) {
                score += 1;
            }
        }

        // Give relevant utility knowledge a stronger ranking.
        if (utilityType != null
                && normalizedSource.equals(utilityType.toLowerCase(
                        Locale.ROOT
                ) + ".txt")) {
            score += 10;
        }

        // Give matching anomaly-rule documentation a ranking boost.
        for (String rule : anomalyRules) {
            if (content.toUpperCase(Locale.ROOT).contains(rule)) {
                score += 3;
            }
        }

        // General billing and anomaly references remain eligible.
        if (normalizedSource.equals("general_billing.txt")
                || normalizedSource.equals("billing_anomalies.txt")) {
            score += 2;
        }

        return score;
    }

    /**
     * Returns true when a source is specifically for a different utility.
     * Non-utility-specific files are not rejected by this method.
     */
    private boolean isWrongUtilitySource(
            String source,
            String requestedUtility
    ) {
        String sourceUtility = utilityFromSource(source);

        if (sourceUtility == null) {
            return false;
        }

        // If a utility was supplied, exclude all other utility files.
        return requestedUtility != null
                && !requestedUtility.equals(sourceUtility);
    }

    private String utilityFromSource(String source) {
        return switch (source) {
            case "electricity.txt" -> "ELECTRICITY";
            case "water.txt" -> "WATER";
            case "internet.txt" -> "INTERNET";
            default -> null;
        };
    }

    private String normalizeUtility(String utilityType) {
        if (utilityType == null || utilityType.isBlank()) {
            return null;
        }

        String normalized = utilityType.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ELECTRICITY", "WATER", "INTERNET" -> normalized;
            default -> null;
        };
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> tokens = new HashSet<>();
        var matcher = TOKEN_PATTERN.matcher(
                text.toLowerCase(Locale.ROOT)
        );

        while (matcher.find()) {
            String token = matcher.group();

            if (token.length() > 1 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private record ScoredChunk(
            RagChunk chunk,
            int score
    ) {}
}
