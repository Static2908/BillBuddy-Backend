package com.example.backend.repository;

import com.example.backend.entity.Bill;
import com.example.backend.entity.User;
import com.example.backend.enums.UtilityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class BillRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillRepository billRepository;

    @Test
    void shouldFilterAndSortBillsByUserAndUtilityType() {
        User user = userRepository.save(new User(
                "Amit Sharma",
                "amit-bills@example.com",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDE123456"
        ));

        Bill mayBill = new Bill(
                user,
                UtilityType.ELECTRICITY,
                "City Power",
                LocalDate.of(2026, 5, 1),
                new BigDecimal("2540.75")
        );
        Bill julyBill = new Bill(
                user,
                UtilityType.ELECTRICITY,
                "City Power",
                LocalDate.of(2026, 7, 1),
                new BigDecimal("2988.10")
        );
        Bill waterBill = new Bill(
                user,
                UtilityType.WATER,
                "Metro Water",
                LocalDate.of(2026, 7, 1),
                new BigDecimal("870.00")
        );

        billRepository.saveAll(List.of(mayBill, julyBill, waterBill));

        List<Bill> electricityBills = billRepository.findByUserIdAndUtilityTypeOrderByBillingPeriodDesc(
                user.getId(),
                UtilityType.ELECTRICITY
        );

        assertEquals(2, electricityBills.size());
        assertEquals(LocalDate.of(2026, 7, 1), electricityBills.get(0).getBillingPeriod());
    }
}
