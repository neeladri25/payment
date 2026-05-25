package com.yuno.payment.repository;

import com.yuno.payment.entity.PaymentProvider;
import com.yuno.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {
    Optional<PaymentProvider> findBySupportedMethodAndIsActive(PaymentMethod method, Boolean isActive);
}

