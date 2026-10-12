package com.example.backend.repository;

import com.example.backend.entity.Bill;
import com.example.backend.enums.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {
    java.util.Optional<Bill> findByIdAndUserId(Long id, Long userId);

    List<Bill> findByUserIdOrderByBillingPeriodDesc(Long userId);

    List<Bill> findByUserIdAndUtilityTypeOrderByBillingPeriodDesc(Long userId, UtilityType utilityType);
    void deleteByUserId(Long userId);
}
