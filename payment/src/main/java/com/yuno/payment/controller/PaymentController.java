package com.yuno.payment.controller;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment.
     * POST /v1/payments
     * Includes idempotency support via x-idempotency-key header.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        log.info("Received payment creation request with idempotency key: {}", request.idempotencyKey());

        PaymentResponse response = paymentService.createPayment(request);

        log.info("Payment created successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment status by ID.
     * GET /v1/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
        log.info("Received request to fetch payment with id: {}", id);

        PaymentResponse response = paymentService.getPayment(id);

        log.info("Payment fetched successfully with id: {}", id);
        return ResponseEntity.ok(response);
    }
}

