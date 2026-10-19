package com.diluwar.order.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.diluwar.order.dto.PaymentResponse;

/**
 * Declarative OpenFeign client communicating with Payment Service instances discovered via Netflix Eureka.
 * <p>
 * Load Balancing & Service Discovery:
 * - Client-Side Load Balancing: Integrates natively with Spring Cloud LoadBalancer to distribute requests
 *   across registered healthy instances of 'payment-service'.
 * - Dynamic Discovery: Resolves hostnames and port allocations at runtime via Eureka Service Registry,
 *   eliminating hardcoded endpoint URIs and supporting dynamic autoscaling.
 * - Idempotency Preservation: Seamlessly transmits the 'Idempotency-Key' HTTP header across balanced instances.
 */
@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentFeignClient {

    @PostMapping
    PaymentResponse createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentRequest request
    );

    @GetMapping("/order/{orderId}")
    PaymentResponse getPaymentByOrderId(@PathVariable("orderId") Long orderId);

    record CreatePaymentRequest(
            Long orderId,
            BigDecimal amount,
            String currency
    ) {
    }
}
