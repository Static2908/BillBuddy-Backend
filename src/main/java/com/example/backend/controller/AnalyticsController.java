package com.example.backend.controller;

import com.example.backend.dto.AnalyticsDtos;
import com.example.backend.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/bills/{id}/comparison")
    public AnalyticsDtos.Comparison compare(
            Authentication authentication, @PathVariable Long id, @RequestParam Long previousBillId
    ) {
        return analyticsService.compare(authentication.getName(), id, previousBillId);
    }

    @GetMapping("/analytics/summary")
    public AnalyticsDtos.Summary summary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        return analyticsService.summary(authentication.getName(), startDate, endDate);
    }
}
