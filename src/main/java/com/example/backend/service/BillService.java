package com.example.backend.service;

import com.example.backend.dto.BillDtos;
import com.example.backend.entity.Bill;
import com.example.backend.entity.BillLineItem;
import com.example.backend.entity.User;
import com.example.backend.enums.BillStatus;
import com.example.backend.enums.UtilityType;
import com.example.backend.exception.ApiExceptions.ResourceNotFoundException;
import com.example.backend.repository.BillLineItemRepository;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class BillService {
    private final BillRepository billRepository;
    private final BillLineItemRepository lineItemRepository;
    private final UserRepository userRepository;

    public BillService(BillRepository billRepository, BillLineItemRepository lineItemRepository, UserRepository userRepository) {
        this.billRepository = billRepository;
        this.lineItemRepository = lineItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BillDtos.BillResponse create(String email, BillDtos.BillRequest request) {
        User user = getUser(email);
        Bill bill = new Bill(user, request.utilityType(), request.providerName().trim(),
                request.billingPeriod(), request.totalAmount());
        apply(bill, request);
        Bill saved = billRepository.save(bill);
        saveLineItems(saved, request.lineItems());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BillDtos.PageResponse<BillDtos.BillResponse> list(
            String email, UtilityType utilityType, BillStatus status,
            LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        Long userId = getUser(email).getId();
        Specification<Bill> specification = (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
        if (utilityType != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("utilityType"), utilityType));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (startDate != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("billingPeriod"), startDate));
        }
        if (endDate != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("billingPeriod"), endDate));
        }
        Page<BillDtos.BillResponse> page = billRepository.findAll(specification, pageable).map(this::toResponse);
        return new BillDtos.PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public BillDtos.BillResponse get(String email, Long id) {
        return toResponse(findOwned(email, id));
    }

    @Transactional
    public BillDtos.BillResponse update(String email, Long id, BillDtos.BillRequest request) {
        Bill bill = findOwned(email, id);
        apply(bill, request);
        lineItemRepository.deleteByBillId(id);
        saveLineItems(bill, request.lineItems());
        return toResponse(billRepository.save(bill));
    }

    @Transactional
    public void delete(String email, Long id) {
        Bill bill = findOwned(email, id);
        lineItemRepository.deleteByBillId(id);
        billRepository.delete(bill);
    }

    @Transactional
    public BillDtos.BillResponse updateStatus(String email, Long id, BillDtos.StatusRequest request) {
        Bill bill = findOwned(email, id);
        bill.setStatus(request.status());
        return toResponse(billRepository.save(bill));
    }

    @Transactional
    public BillDtos.LineItemResponse addLineItem(String email, Long billId, BillDtos.LineItemRequest request) {
        Bill bill = findOwned(email, billId);
        return toLineItem(lineItemRepository.save(new BillLineItem(bill, request.description().trim(),
                request.category().trim(), request.amount())));
    }

    @Transactional
    public BillDtos.LineItemResponse updateLineItem(String email, Long billId, Long itemId, BillDtos.LineItemRequest request) {
        findOwned(email, billId);
        BillLineItem item = lineItemRepository.findByIdAndBillId(itemId, billId)
                .orElseThrow(() -> new ResourceNotFoundException("Line item not found"));
        item.setDescription(request.description().trim());
        item.setCategory(request.category().trim());
        item.setAmount(request.amount());
        return toLineItem(lineItemRepository.save(item));
    }

    @Transactional
    public void deleteLineItem(String email, Long billId, Long itemId) {
        findOwned(email, billId);
        BillLineItem item = lineItemRepository.findByIdAndBillId(itemId, billId)
                .orElseThrow(() -> new ResourceNotFoundException("Line item not found"));
        lineItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Bill findOwned(String email, Long id) {
        User user = getUser(email);
        return billRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    @Transactional(readOnly = true)
    public Long findOwnedUserId(String email) {
        return getUser(email).getId();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void apply(Bill bill, BillDtos.BillRequest request) {
        bill.setUtilityType(request.utilityType());
        bill.setProviderName(request.providerName().trim());
        bill.setBillingPeriod(request.billingPeriod());
        bill.setTotalAmount(request.totalAmount());
        bill.setUsageAmount(request.usageAmount());
        bill.setUsageUnit(request.usageUnit() == null ? null : request.usageUnit().trim());
        bill.setDueDate(request.dueDate());
        bill.setStatus(request.status());
    }

    private void saveLineItems(Bill bill, List<BillDtos.LineItemRequest> requests) {
        if (requests == null) {
            return;
        }
        requests.forEach(request -> lineItemRepository.save(new BillLineItem(bill, request.description().trim(),
                request.category().trim(), request.amount())));
    }

    public BillDtos.BillResponse toResponse(Bill bill) {
        List<BillDtos.LineItemResponse> items = lineItemRepository.findByBillId(bill.getId())
                .stream().map(this::toLineItem).toList();
        return new BillDtos.BillResponse(bill.getId(), bill.getUtilityType(), bill.getProviderName(),
                bill.getBillingPeriod(), bill.getTotalAmount(), bill.getUsageAmount(), bill.getUsageUnit(),
                bill.getDueDate(), bill.getStatus(), bill.getCreatedAt(), items);
    }

    private BillDtos.LineItemResponse toLineItem(BillLineItem item) {
        return new BillDtos.LineItemResponse(item.getId(), item.getDescription(), item.getCategory(), item.getAmount());
    }
}
