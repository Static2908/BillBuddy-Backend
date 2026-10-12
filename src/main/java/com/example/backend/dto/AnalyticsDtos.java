package com.example.backend.dto;

import com.example.backend.enums.UtilityType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record Comparison(
            Long previousBillId, Long currentBillId,
            BigDecimal previousAmount, BigDecimal currentAmount, BigDecimal amountDifference,
            BigDecimal amountChangePercent, BigDecimal previousUsage, BigDecimal currentUsage,
            BigDecimal usageDifference, BigDecimal usageChangePercent,
            BigDecimal previousCostPerUnit, BigDecimal currentCostPerUnit,
            boolean comparable, List<String> notes
    ) {
    }

    public record Anomaly(Long billId, String ruleCode, String title, String severity, String evidence, BigDecimal threshold) {
    }

    public record MonthlySpend(String month, BigDecimal amount) {
    }

    public record UtilitySpend(UtilityType utilityType, BigDecimal amount) {
    }

    public record DueBill(Long id, String providerName, LocalDate dueDate, BigDecimal amount) {
    }

    public record Summary(
            BigDecimal totalSpending,
            long paidCount, long unpaidCount, long overdueCount,
            BigDecimal paidAmount, BigDecimal unpaidAmount, BigDecimal overdueAmount,
            List<MonthlySpend> monthlySpending,
            List<UtilitySpend> spendingByUtility,
            List<DueBill> upcomingDueDates,
            List<BillDtos.BillResponse> recentBills,
            List<Anomaly> anomalies
    ) {
    }
}
