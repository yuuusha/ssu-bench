package com.diev.handler;

import com.diev.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public void confirmTask(
            @RequestParam UUID taskId,
            @RequestParam UUID customerId
    ) {
        paymentService.confirmTask(taskId, customerId);
    }
}