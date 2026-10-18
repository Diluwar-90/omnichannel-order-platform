package com.diluwar.gateway.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Edge Reactive Circuit Breaker Fallback Controller.
 * <p>
 * Handles graceful degradation at the Spring Cloud Gateway boundary:
 * - Prevents client connection stalls during backend microservice outages or high latency spikes.
 * - Immediately returns structured HTTP 503 (Service Unavailable) JSON responses rather than raw socket drops.
 * - Conforms to cloud-native zero-trust edge architectural patterns.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/orders")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        return buildFallbackResponse("Order Service is currently unavailable. Circuit breaker triggered.");
    }

    @RequestMapping("/payments")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        return buildFallbackResponse("Payment Service is currently unavailable. Circuit breaker triggered.");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryServiceFallback() {
        return buildFallbackResponse("Inventory Service is currently unavailable. Circuit breaker triggered.");
    }

    @RequestMapping("/products")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        return buildFallbackResponse("Product Service is currently unavailable. Circuit breaker triggered.");
    }

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        return buildFallbackResponse("Auth Service is currently unavailable. Circuit breaker triggered.");
    }

    @RequestMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        return buildFallbackResponse("Notification Service is currently unavailable. Circuit breaker triggered.");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "Service Unavailable",
                        "message", message
                ));
    }
}
