package com.diluwar.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Resilient External Payment Gateway Adapter.
 * <p>
 * Simulates integration with third-party payment providers (e.g., Stripe, Adyen, CyberSource).
 * Protected by Resilience4j Circuit Breaker and automated Retries to isolate core financial transaction
 * workflows from external provider API volatility, latency degradation, or partner outages.
 */
@Component
public class DefaultPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentProvider.class);

    @Override
    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentProvider")
    public PaymentProviderResult processPayment(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {

        log.info("Contacting external payment provider: orderId={}, amount={} {}, idempotencyKey={}",
                orderId, amount, currency, idempotencyKey);

        String transactionId = "txn_" + UUID.randomUUID();
        return PaymentProviderResult.success(transactionId);
    }

    /**
     * Circuit breaker fallback invoked when payment provider reaches failure thresholds.
     */
    public PaymentProviderResult processPaymentFallback(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Throwable throwable) {

        log.error("Payment provider circuit breaker/retry fallback triggered: orderId={}, error={}",
                orderId, throwable.getMessage());
        return PaymentProviderResult.failure("External payment provider unavailable: " + throwable.getMessage());
    }
}
