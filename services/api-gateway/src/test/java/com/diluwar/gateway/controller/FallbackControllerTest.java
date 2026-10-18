package com.diluwar.gateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    @Test
    void shouldReturnServiceUnavailableForOrderFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.orderServiceFallback();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order Service is currently unavailable. Circuit breaker triggered.", response.getBody().get("message"));
    }

    @Test
    void shouldReturnServiceUnavailableForPaymentFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.paymentServiceFallback();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Payment Service is currently unavailable. Circuit breaker triggered.", response.getBody().get("message"));
    }

    @Test
    void shouldReturnServiceUnavailableForInventoryFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.inventoryServiceFallback();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Inventory Service is currently unavailable. Circuit breaker triggered.", response.getBody().get("message"));
    }
}
