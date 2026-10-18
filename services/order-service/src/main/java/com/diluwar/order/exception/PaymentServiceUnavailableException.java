package com.diluwar.order.exception;

/**
 * Exception thrown when the downstream Payment Service is unreachable or when
 * the Resilience4j Circuit Breaker transitions to OPEN state.
 * Allows the Order orchestration engine to safely handle transient degradation without data corruption.
 */
public class PaymentServiceUnavailableException extends RuntimeException {

    public PaymentServiceUnavailableException(String message) {
        super(message);
    }

    public PaymentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
