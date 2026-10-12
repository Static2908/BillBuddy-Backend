package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ExplanationDtos {

    private ExplanationDtos() {
    }

    public record AnomalyEvidence(
            Long billId,
            String ruleCode,
            String title,
            String severity,
            String evidence,
            BigDecimal threshold
    ) {
    }

    public record SourceReference(
            String source,
            String content
    ) {
    }

    public record ExplanationResponse(
            Long billId,
            List<AnomalyEvidence> anomalies,
            List<SourceReference> sources,
            String explanation
    ) {
    }
}
