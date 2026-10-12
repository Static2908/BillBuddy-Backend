package com.example.backend.repository;

import com.example.backend.entity.BillLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillLineItemRepository extends JpaRepository<BillLineItem, Long> {
    List<BillLineItem> findByBillId(Long billId);

    java.util.Optional<BillLineItem> findByIdAndBillId(Long id, Long billId);

    void deleteByBillId(Long billId);
    void deleteByBillUserId(Long userId);
}
