package com.diluwar.payment.exception;

/**
 * Exception thrown when external payment processing fails, times out, or triggers
 * a Resilience4j circuit breaker fallback during provider execution.
 */
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
