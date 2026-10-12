package com.example.backend.dto;

import com.example.backend.enums.BillStatus;
import com.example.backend.enums.UtilityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BillDtos {
    private BillDtos() {
    }

    public record LineItemRequest(
            @NotBlank @Size(max = 100) String description,
            @NotBlank @Size(max = 40) String category,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount
    ) {
    }

    public record BillRequest(
            @NotNull UtilityType utilityType,
            @NotBlank @Size(max = 100) String providerName,
            @NotNull LocalDate billingPeriod,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
            @DecimalMin("0.00") @Digits(integer = 9, fraction = 3) BigDecimal usageAmount,
            @Size(max = 20) String usageUnit,
            LocalDate dueDate,
            @NotNull BillStatus status,
            @Valid List<LineItemRequest> lineItems
    ) {
    }

    public record StatusRequest(@NotNull BillStatus status) {
    }

    public record LineItemResponse(Long id, String description, String category, BigDecimal amount) {
    }

    public record BillResponse(
            Long id,
            UtilityType utilityType,
            String providerName,
            LocalDate billingPeriod,
            BigDecimal totalAmount,
            BigDecimal usageAmount,
            String usageUnit,
            LocalDate dueDate,
            BillStatus status,
            LocalDateTime createdAt,
            List<LineItemResponse> lineItems
    ) {
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
    }
}
