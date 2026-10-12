package com.example.backend.service;

import com.example.backend.dto.AnalyticsDtos;
import com.example.backend.dto.BillDtos;
import com.example.backend.entity.Bill;
import com.example.backend.enums.BillStatus;
import com.example.backend.repository.BillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final BillService billService;
    private final BillRepository billRepository;
    private final AnomalyService anomalyService;

    public AnalyticsService(BillService billService,BillRepository billRepository,AnomalyService anomalyService) {
    this.billService = billService;
    this.billRepository = billRepository;
    this.anomalyService = anomalyService;
}

    @Transactional(readOnly = true)
    public AnalyticsDtos.Comparison compare(String email, Long currentId, Long previousId) {
        Bill current = billService.findOwned(email, currentId);
        Bill previous = billService.findOwned(email, previousId);
        List<String> notes = new ArrayList<>();
        boolean comparable = current.getUtilityType() == previous.getUtilityType()
                && Objects.equals(current.getUsageUnit(), previous.getUsageUnit());
        if (current.getUtilityType() != previous.getUtilityType()) notes.add("Utility types differ");
        if (!Objects.equals(current.getUsageUnit(), previous.getUsageUnit())) notes.add("Usage units differ");
        return new AnalyticsDtos.Comparison(previous.getId(), current.getId(), previous.getTotalAmount(),
                current.getTotalAmount(), current.getTotalAmount().subtract(previous.getTotalAmount()),
                percentChange(previous.getTotalAmount(), current.getTotalAmount()),
                previous.getUsageAmount(), current.getUsageAmount(),
                difference(previous.getUsageAmount(), current.getUsageAmount()),
                percentChange(previous.getUsageAmount(), current.getUsageAmount()),
                costPerUnit(previous), costPerUnit(current), comparable, notes);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.Summary summary(String email, LocalDate start, LocalDate end) {
        Long userId = billService.findOwnedUserId(email);
        var spec = (org.springframework.data.jpa.domain.Specification<Bill>) (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
        if (start != null) spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("billingPeriod"), start));
        if (end != null) spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("billingPeriod"), end));
        List<Bill> bills = billRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "billingPeriod"));
        BigDecimal total = sum(bills);
        return new AnalyticsDtos.Summary(total,
                count(bills, BillStatus.PAID), count(bills, BillStatus.UNPAID), count(bills, BillStatus.OVERDUE),
                sumStatus(bills, BillStatus.PAID), sumStatus(bills, BillStatus.UNPAID), sumStatus(bills, BillStatus.OVERDUE),
                bills.stream().collect(Collectors.groupingBy(b -> YearMonth.from(b.getBillingPeriod()), TreeMap::new,
                                Collectors.reducing(BigDecimal.ZERO, Bill::getTotalAmount, BigDecimal::add)))
                        .entrySet().stream().map(e -> new AnalyticsDtos.MonthlySpend(e.getKey().toString(), e.getValue())).toList(),
                bills.stream().collect(Collectors.groupingBy(Bill::getUtilityType,
                                Collectors.reducing(BigDecimal.ZERO, Bill::getTotalAmount, BigDecimal::add)))
                        .entrySet().stream().map(e -> new AnalyticsDtos.UtilitySpend(e.getKey(), e.getValue())).toList(),
                bills.stream().filter(b -> b.getDueDate() != null && !b.getDueDate().isBefore(LocalDate.now())
                                && b.getStatus() != BillStatus.PAID).limit(5)
                        .map(b -> new AnalyticsDtos.DueBill(b.getId(), b.getProviderName(), b.getDueDate(), b.getTotalAmount())).toList(),
                bills.stream().limit(5).map(billService::toResponse).toList(),
                anomalyService.detect(bills));
    }

    private BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.signum() == 0 || current == null) return null;
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal difference(BigDecimal previous, BigDecimal current) {
        return previous == null || current == null ? null : current.subtract(previous);
    }

    private BigDecimal costPerUnit(Bill bill) {
        if (bill.getUsageAmount() == null || bill.getUsageAmount().signum() == 0) return null;
        return bill.getTotalAmount().divide(bill.getUsageAmount(), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<Bill> bills) {
        return bills.stream().map(Bill::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(List<Bill> bills, BillStatus status) {
        return bills.stream().filter(b -> b.getStatus() == status).count();
    }

    private BigDecimal sumStatus(List<Bill> bills, BillStatus status) {
        return bills.stream().filter(b -> b.getStatus() == status).map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
