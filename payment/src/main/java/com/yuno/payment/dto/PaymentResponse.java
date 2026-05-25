package com.yuno.payment.dto;

import com.yuno.payment.entity.PaymentStatus;
import com.yuno.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String idempotencyKey,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String provider,
        String description,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

