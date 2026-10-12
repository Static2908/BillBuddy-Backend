package com.example.backend.rag;

import com.example.backend.dto.ExplanationDtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillExplanationController {

    private final BillExplanationService explanationService;

    public BillExplanationController(
            BillExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    @GetMapping("/{id}/explanation")
    public ExplanationDtos.ExplanationResponse explainBill(
            Authentication authentication,
            @PathVariable Long id) {

        return explanationService.explainBill(
                authentication.getName(),
                id
        );
    }
}