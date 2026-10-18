package com.diluwar.order.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.diluwar.order.dto.PaymentResponse;
import com.diluwar.order.exception.PaymentServiceUnavailableException;

class PaymentClientTest {

    private RestClient restClient;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        paymentClient = new PaymentClient(restClient);
    }

    @Test
    void shouldInvokeCreatePaymentFallbackGracefully() {
        Throwable cause = new RuntimeException("Connection refused");

        PaymentServiceUnavailableException exception = assertThrows(
                PaymentServiceUnavailableException.class,
                () -> paymentClient.createPaymentFallback(
                        101L,
                        BigDecimal.valueOf(250.00),
                        "USD",
                        "custom-idempotency-key",
                        cause
                )
        );

        assertNotNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldInvokeGetPaymentFallbackGracefully() {
        Throwable cause = new RuntimeException("Read timed out");

        PaymentServiceUnavailableException exception = assertThrows(
                PaymentServiceUnavailableException.class,
                () -> paymentClient.getPaymentFallback(101L, cause)
        );

        assertNotNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
