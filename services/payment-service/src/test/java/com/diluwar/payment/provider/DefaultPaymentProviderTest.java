package com.diluwar.payment.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPaymentProviderTest {

    private DefaultPaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
        paymentProvider = new DefaultPaymentProvider();
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        PaymentProviderResult result = paymentProvider.processPayment(
                1001L,
                BigDecimal.valueOf(199.99),
                "USD",
                "idemp-test-1"
        );

        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("txn_"));
    }

    @Test
    void shouldHandleFallbackGracefully() {
        Throwable cause = new RuntimeException("Gateway timed out");

        PaymentProviderResult result = paymentProvider.processPaymentFallback(
                1001L,
                BigDecimal.valueOf(199.99),
                "USD",
                "idemp-test-1",
                cause
        );

        assertFalse(result.success());
        assertTrue(result.message().contains("External payment provider unavailable"));
    }
}
