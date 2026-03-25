package com.diev.controller;

import com.diev.security.CurrentUserId;
import com.diev.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public void confirmTask(
            @RequestParam UUID taskId,
            @CurrentUserId UUID customerId
    ) {
        paymentService.confirmTask(taskId, customerId);
    }
}