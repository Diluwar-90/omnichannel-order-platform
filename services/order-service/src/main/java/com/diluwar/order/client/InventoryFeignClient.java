package com.diluwar.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative OpenFeign client for Inventory Service with Spring Cloud LoadBalancer integration.
 * <p>
 * Load Balancing & Service Discovery:
 * - Client-Side Discovery & Routing: Dispatches stock reservation and compensation calls to healthy
 *   'inventory-service' instances supplied by Netflix Eureka.
 * - Non-blocking Round-Robin: Spring Cloud LoadBalancer guarantees uniform distribution across inventory nodes.
 */
@FeignClient(name = "inventory-service", path = "/api/v1/inventory", url = "${inventory.service.url:}")
public interface InventoryFeignClient {

    @PostMapping("/{productId}/reserve")
    void reserveStock(
            @PathVariable("productId") Long productId,
            @RequestBody QuantityRequest request
    );

    @PostMapping("/{productId}/release")
    void releaseStock(
            @PathVariable("productId") Long productId,
            @RequestBody QuantityRequest request
    );

    record QuantityRequest(Integer quantity) {
    }
}
