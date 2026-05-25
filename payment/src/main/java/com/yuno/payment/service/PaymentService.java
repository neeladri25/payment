package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.entity.Payment;
import com.yuno.payment.entity.PaymentMethod;
import com.yuno.payment.entity.PaymentProvider;
import com.yuno.payment.entity.PaymentStatus;
import com.yuno.payment.repository.PaymentRepository;
import com.yuno.payment.repository.PaymentProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRepository paymentProviderRepository;

    /**
     * Create a payment with idempotency support.
     * Using @Cacheable on idempotency key to prevent duplicate processing.
     * If the same idempotency key is used, the cached result is returned.
     */
    @Transactional
    @Cacheable(value = "idempotencyCache", key = "#request.idempotencyKey")
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment with idempotency key: {}", request.idempotencyKey());

        // Check if payment already exists with this idempotency key
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.info("Payment already exists with idempotency key: {}", request.idempotencyKey());
            return mapToResponse(existingPayment.get());
        }

        // Route payment based on payment method
        PaymentProvider provider = routePayment(request.paymentMethod());
        if (provider == null) {
            log.error("No active provider found for payment method: {}", request.paymentMethod());
            throw new IllegalArgumentException("No active provider available for payment method: " + request.paymentMethod());
        }

        // Create payment record
        Payment payment = Payment.builder()
                .idempotencyKey(request.idempotencyKey())
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .provider(provider.getName())
                .paymentProvider(provider)
                .description(request.description())
                .build();

        // Save initial payment
        payment = paymentRepository.save(payment);
        log.info("Payment created with id: {}, status: {}", payment.getId(), payment.getStatus());

        // Attempt to process payment with resilience
        processPaymentWithResilience(payment);

        // Reload to get updated status
        payment = paymentRepository.findById(payment.getId()).orElseThrow();

        return mapToResponse(payment);
    }

    /**
     * Route payment to the appropriate provider based on payment method.
     * CARD -> Provider A
     * UPI -> Provider B
     */
    private PaymentProvider routePayment(PaymentMethod paymentMethod) {
        log.debug("Routing payment with method: {}", paymentMethod);

        return switch (paymentMethod) {
            case CARD -> {
                log.debug("CARD payment detected, routing to Provider A");
                yield paymentProviderRepository.findBySupportedMethodAndIsActive(PaymentMethod.CARD, true).orElse(null);
            }
            case UPI -> {
                log.debug("UPI payment detected, routing to Provider B");
                yield paymentProviderRepository.findBySupportedMethodAndIsActive(PaymentMethod.UPI, true).orElse(null);
            }
            case WALLET, BANK_TRANSFER -> {
                log.debug("Payment method {} routed to default provider", paymentMethod);
                yield paymentProviderRepository.findBySupportedMethodAndIsActive(paymentMethod, true).orElse(null);
            }
        };
    }

    /**
     * Process payment with resilience and failover mechanism.
     * Uses try-catch to handle provider failures gracefully.
     */
    private void processPaymentWithResilience(Payment payment) {
        try {
            log.info("Processing payment id: {} with provider: {}", payment.getId(), payment.getProvider());

            // Update status to PROCESSING
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // Simulate provider call (in production, this would be an actual API call)
            callPaymentProvider(payment);

            // Mark as successful
            payment.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment id: {} processed successfully", payment.getId());

        } catch (Exception e) {
            // Handle failure with resilience
            log.error("Payment processing failed for id: {}. Error: {}", payment.getId(), e.getMessage(), e);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());

            // In a real scenario, you could implement a fallback mechanism here
            // Example: try alternative provider, queue for retry, etc.
            log.warn("Payment id: {} marked as FAILED. Reason: {}", payment.getId(), e.getMessage());
        } finally {
            paymentRepository.save(payment);
        }
    }

    /**
     * Simulate calling the payment provider.
     * In production, this would make actual HTTP calls to the provider API.
     */
    private void callPaymentProvider(Payment payment) throws Exception {
        // Simulate provider processing
        log.debug("Calling provider: {} for payment id: {}", payment.getProvider(), payment.getId());

        // Simulate potential failures (for demo purposes)
        // In production, actual API calls would happen here
        if (Math.random() > 0.9) { // 10% failure rate for demo
            throw new RuntimeException("Provider API returned an error");
        }

        log.debug("Provider {} successfully processed payment {}", payment.getProvider(), payment.getId());
    }

    /**
     * Fetch payment status by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String id) {
        log.info("Fetching payment with id: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Payment not found with id: {}", id);
                    return new IllegalArgumentException("Payment not found with id: " + id);
                });

        return mapToResponse(payment);
    }

    /**
     * Map Payment entity to PaymentResponse DTO.
     */
    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getDescription(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

