package com.example.backend.controller;

import com.example.backend.dto.BillDtos;
import com.example.backend.enums.BillStatus;
import com.example.backend.enums.UtilityType;
import com.example.backend.service.BillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/bills")
public class BillController {
    private static final Set<String> SORT_FIELDS = Set.of("billingPeriod", "createdAt", "totalAmount", "dueDate");
    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillDtos.BillResponse create(Authentication authentication, @Valid @RequestBody BillDtos.BillRequest request) {
        return billService.create(authentication.getName(), request);
    }

    @GetMapping
    public BillDtos.PageResponse<BillDtos.BillResponse> list(
            Authentication authentication,
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "billingPeriod,desc") String sort
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        String[] sortParts = sort.split(",", 2);
        if (!SORT_FIELDS.contains(sortParts[0])) {
            throw new IllegalArgumentException("Unsupported sort field");
        }
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        return billService.list(authentication.getName(), utilityType, status, startDate, endDate, pageable);
    }

    @GetMapping("/{id}")
    public BillDtos.BillResponse get(Authentication authentication, @PathVariable Long id) {
        return billService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public BillDtos.BillResponse update(
            Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody BillDtos.BillRequest request
    ) {
        return billService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        billService.delete(authentication.getName(), id);
    }

    @PatchMapping("/{id}/status")
    public BillDtos.BillResponse updateStatus(
            Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody BillDtos.StatusRequest request
    ) {
        return billService.updateStatus(authentication.getName(), id, request);
    }

    @PostMapping("/{billId}/line-items")
    @ResponseStatus(HttpStatus.CREATED)
    public BillDtos.LineItemResponse addLineItem(
            Authentication authentication, @PathVariable Long billId,
            @Valid @RequestBody BillDtos.LineItemRequest request
    ) {
        return billService.addLineItem(authentication.getName(), billId, request);
    }

    @PutMapping("/{billId}/line-items/{itemId}")
    public BillDtos.LineItemResponse updateLineItem(
            Authentication authentication, @PathVariable Long billId, @PathVariable Long itemId,
            @Valid @RequestBody BillDtos.LineItemRequest request
    ) {
        return billService.updateLineItem(authentication.getName(), billId, itemId, request);
    }

    @DeleteMapping("/{billId}/line-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLineItem(Authentication authentication, @PathVariable Long billId, @PathVariable Long itemId) {
        billService.deleteLineItem(authentication.getName(), billId, itemId);
    }
}
