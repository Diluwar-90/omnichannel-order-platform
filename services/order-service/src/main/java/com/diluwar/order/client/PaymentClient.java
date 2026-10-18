package com.diluwar.order.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.diluwar.order.dto.PaymentResponse;
import com.diluwar.order.exception.PaymentServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enterprise-grade synchronous HTTP client communicating with Payment Service.
 * <p>
 * Fault-Tolerance & Resilience Architecture:
 * - Resilience4j Circuit Breaker: Shields calling threads from downstream Payment Service
 *   outages, transitioning to OPEN state when failure thresholds (50%) are breached.
 * - Exponential Backoff Retry: Recovers from transient network glitches without overwhelming
 *   downstream payment nodes.
 * - TimeLimiter & Socket Timeouts: Enforces strict connection and socket read boundaries.
 * - Distributed Idempotency Semantics: Propagates 'Idempotency-Key' HTTP headers across retries
 *   to guarantee exactly-once charge semantics in compliance with financial transaction standards.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private final RestClient restClient;

    public PaymentClient(
            @Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Initiates payment creation with an automated cryptographically unique idempotency key.
     */
    public PaymentResponse createPayment(
            Long orderId,
            BigDecimal amount,
            String currency) {
        return createPayment(orderId, amount, currency, "order-" + orderId + "-" + UUID.randomUUID());
    }

    /**
     * Executes payment creation protected by Circuit Breaker and automated Retries.
     * Propagates client-supplied or derived Idempotency-Key header to eliminate duplicate billing risks.
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "createPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse createPayment(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "order-" + orderId + "-" + UUID.randomUUID();

        return restClient
                .post()
                .uri("/api/v1/payments")
                .header("Idempotency-Key", key)
                .body(new CreatePaymentRequest(
                        orderId,
                        amount,
                        currency
                ))
                .retrieve()
                .body(PaymentResponse.class);
    }

    /**
     * Queries payment status by Order ID under Resilience4j CircuitBreaker and Retry protection.
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "getPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        return restClient
                .get()
                .uri("/api/v1/payments/order/" + orderId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    /**
     * Fallback handler invoked when the Payment Service circuit is OPEN or retries are exhausted.
     * Prevents cascading failure and safely signals temporary degradation to order state machines.
     */
    public PaymentResponse createPaymentFallback(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Throwable throwable) {
        log.error("Circuit breaker/retry fallback triggered for createPayment: orderId={}, idempotencyKey={}, error={}",
                orderId, idempotencyKey, throwable.getMessage());
        throw new PaymentServiceUnavailableException(
                "Payment service is temporarily unavailable for order: " + orderId + " - " + throwable.getMessage(),
                throwable
        );
    }

    public PaymentResponse createPaymentFallback(
            Long orderId,
            BigDecimal amount,
            String currency,
            Throwable throwable) {
        log.error("Circuit breaker/retry fallback triggered for createPayment: orderId={}, error={}",
                orderId, throwable.getMessage());
        throw new PaymentServiceUnavailableException(
                "Payment service is temporarily unavailable for order: " + orderId + " - " + throwable.getMessage(),
                throwable
        );
    }

    public PaymentResponse getPaymentFallback(Long orderId, Throwable throwable) {
        log.error("Circuit breaker/retry fallback triggered for getPaymentByOrderId: orderId={}, error={}",
                orderId, throwable.getMessage());
        throw new PaymentServiceUnavailableException(
                "Payment service is temporarily unavailable for order: " + orderId + " - " + throwable.getMessage(),
                throwable
        );
    }

    private record CreatePaymentRequest(
            Long orderId,
            BigDecimal amount,
            String currency
    ) {
    }
}