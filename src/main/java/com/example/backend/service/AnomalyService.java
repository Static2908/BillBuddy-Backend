package com.example.backend.service;

import com.example.backend.dto.AnalyticsDtos;
import com.example.backend.entity.Bill;
import com.example.backend.enums.UtilityType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnomalyService {

    private static final BigDecimal AMOUNT_THRESHOLD = BigDecimal.valueOf(20);
    private static final BigDecimal USAGE_THRESHOLD = BigDecimal.valueOf(20);
    private static final BigDecimal COST_PER_UNIT_THRESHOLD = BigDecimal.valueOf(15);

    public List<AnalyticsDtos.Anomaly> detect(List<Bill> bills) {

        List<AnalyticsDtos.Anomaly> anomalies = new ArrayList<>();

        /*
         * Group bills by utility type first.
         * This prevents electricity, water, and internet
         * bills from being compared with each other.
         */
        Map<UtilityType, List<Bill>> billsByUtility =
                bills.stream()
                        .collect(Collectors.groupingBy(Bill::getUtilityType));

        /*
         * Process each utility independently.
         */
        for (List<Bill> utilityBills : billsByUtility.values()) {

            /*
             * Sort bills chronologically.
             */
            List<Bill> sortedBills = utilityBills.stream()
                    .sorted(Comparator.comparing(Bill::getBillingPeriod))
                    .toList();

            /*
             * Compare bills within the same utility.
             */
            for (int i = 0; i < sortedBills.size(); i++) {

                Bill current = sortedBills.get(i);

                /*
                 * Rule 1:
                 * Zero usage with a non-zero bill amount.
                 */
                if (current.getUsageAmount() != null
                        && current.getUsageAmount().signum() == 0
                        && current.getTotalAmount() != null
                        && current.getTotalAmount().signum() > 0) {

                    anomalies.add(new AnalyticsDtos.Anomaly(
                            current.getId(),
                            "ZERO_USAGE_NONZERO_AMOUNT",
                            "Bill has a non-zero amount with zero usage",
                            "HIGH",
                            "Bill #" + current.getId()
                                    + " has amount " + current.getTotalAmount()
                                    + " but usage is 0 "
                                    + safeUnit(current.getUsageUnit()),
                            BigDecimal.ZERO
                    ));
                }

                /*
                 * If there is no previous bill for this utility,
                 * comparison-based anomaly rules cannot be evaluated.
                 */
                if (i == 0) {
                    continue;
                }

                Bill previous = sortedBills.get(i - 1);

                /*
                 * Usage units must match when both are available.
                 *
                 * Example:
                 * kWh should not be compared with GB or kL.
                 */
                if (current.getUsageUnit() != null
                        && previous.getUsageUnit() != null
                        && !Objects.equals(
                                current.getUsageUnit(),
                                previous.getUsageUnit())) {

                    continue;
                }

                /*
                 * Rule 2:
                 * Bill amount increased by 20% or more.
                 */
                BigDecimal amountChange = percentChange(
                        previous.getTotalAmount(),
                        current.getTotalAmount()
                );

                if (amountChange != null
                        && amountChange.compareTo(AMOUNT_THRESHOLD) >= 0) {

                    anomalies.add(new AnalyticsDtos.Anomaly(
                            current.getId(),
                            "AMOUNT_SPIKE",
                            "Bill amount increased significantly",
                            "HIGH",
                            "Bill amount increased from "
                                    + previous.getTotalAmount()
                                    + " to "
                                    + current.getTotalAmount()
                                    + " (" + amountChange + "%)",
                            AMOUNT_THRESHOLD
                    ));
                }

                /*
                 * Rule 3:
                 * Usage increased by 20% or more.
                 */
                BigDecimal usageChange = percentChange(
                        previous.getUsageAmount(),
                        current.getUsageAmount()
                );

                if (usageChange != null
                        && usageChange.compareTo(USAGE_THRESHOLD) >= 0) {

                    anomalies.add(new AnalyticsDtos.Anomaly(
                            current.getId(),
                            "USAGE_SPIKE",
                            "Usage increased significantly",
                            "MEDIUM",
                            "Usage increased from "
                                    + previous.getUsageAmount()
                                    + " to "
                                    + current.getUsageAmount()
                                    + " "
                                    + safeUnit(current.getUsageUnit())
                                    + " (" + usageChange + "%)",
                            USAGE_THRESHOLD
                    ));
                }

                /*
                 * Rule 4:
                 * Cost per unit increased by 15% or more.
                 */
                BigDecimal previousCost = costPerUnit(previous);
                BigDecimal currentCost = costPerUnit(current);

                BigDecimal costChange = percentChange(
                        previousCost,
                        currentCost
                );

                if (costChange != null
                        && costChange.compareTo(COST_PER_UNIT_THRESHOLD) >= 0) {

                    anomalies.add(new AnalyticsDtos.Anomaly(
                            current.getId(),
                            "COST_PER_UNIT_SPIKE",
                            "Cost per unit increased significantly",
                            "HIGH",
                            "Cost per unit increased from "
                                    + previousCost
                                    + " to "
                                    + currentCost
                                    + " (" + costChange + "%)",
                            COST_PER_UNIT_THRESHOLD
                    ));
                }
            }
        }

        return anomalies;
    }

    /*
     * Calculates percentage change:
     *
     * ((current - previous) / previous) * 100
     */
    private BigDecimal percentChange(
            BigDecimal previous,
            BigDecimal current
    ) {

        if (previous == null
                || current == null
                || previous.signum() == 0) {

            return null;
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        previous,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * Calculates cost per unit:
     *
     * total amount / usage amount
     */
    private BigDecimal costPerUnit(Bill bill) {

        if (bill.getTotalAmount() == null
                || bill.getUsageAmount() == null
                || bill.getUsageAmount().signum() == 0) {

            return null;
        }

        return bill.getTotalAmount()
                .divide(
                        bill.getUsageAmount(),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * Prevents "null" from appearing in anomaly evidence
     * when a bill does not have a usage unit.
     */
    private String safeUnit(String unit) {
        return unit == null ? "" : unit;
    }
}