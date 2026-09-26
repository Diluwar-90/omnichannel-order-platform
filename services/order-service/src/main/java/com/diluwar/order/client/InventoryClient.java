package com.diluwar.order.client;

import com.diluwar.order.exception.InventoryReservationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            @Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

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

    private record QuantityRequest(Integer quantity) {
    }
}