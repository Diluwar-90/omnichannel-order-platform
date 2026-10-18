package com.diluwar.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.diluwar.order.exception.InventoryReservationException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Mission-critical synchronous inventory reservation client.
 * <p>
 * Fault-Tolerance Architecture:
 * - Resilience4j Circuit Breaker: Prevents order service thread saturation during flash sales
 *   or warehouse service maintenance by failing fast when inventory response latencies spike.
 * - Exponential Backoff Retries: Recovers smoothly from temporary concurrency lock contentions.
 * - Graceful Fallback Mechanics: Translates downstream connection errors into structured business
 *   exceptions, allowing clean rollback of pending orders without orphaned DB locks.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private final RestClient restClient;

    public InventoryClient(
            @Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Reserves stock for an order item under Circuit Breaker and automated Retry protection.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveFallback")
    @Retry(name = "inventoryService")
    public void reserve(Long productId, Integer quantity) {
        try {
            restClient.post()
                    .uri(
                            "/api/v1/inventory/{productId}/reserve",
                            productId
                    )
                    .body(new QuantityRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new InventoryReservationException(
                    "Unable to reserve inventory for product: "
                            + productId
                            + " - "
                            + ex.getMessage()
            );
        }
    }

    /**
     * Releases previously reserved stock (compensating transaction) during payment failure or order cancellation.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseFallback")
    @Retry(name = "inventoryService")
    public void release(Long productId, Integer quantity) {
        try {
            restClient.post()
                    .uri(
                            "/api/v1/inventory/{productId}/release",
                            productId
                    )
                    .body(new QuantityRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new InventoryReservationException(
                    "Unable to release inventory for product: "
                            + productId
                            + " - "
                            + ex.getMessage()
            );
        }
    }

    /**
     * Circuit breaker fallback for inventory reservation.
     */
    public void reserveFallback(Long productId, Integer quantity, Throwable throwable) {
        log.error("Circuit breaker/retry fallback triggered for inventory reserve: productId={}, quantity={}, error={}",
                productId, quantity, throwable.getMessage());
        throw new InventoryReservationException(
                "Inventory service is temporarily unavailable for product: " + productId + " - " + throwable.getMessage()
        );
    }

    /**
     * Circuit breaker fallback for inventory release compensation.
     */
    public void releaseFallback(Long productId, Integer quantity, Throwable throwable) {
        log.error("Circuit breaker/retry fallback triggered for inventory release: productId={}, quantity={}, error={}",
                productId, quantity, throwable.getMessage());
        throw new InventoryReservationException(
                "Inventory service is temporarily unavailable for product: " + productId + " - " + throwable.getMessage()
        );
    }

    private record QuantityRequest(Integer quantity) {
    }
}