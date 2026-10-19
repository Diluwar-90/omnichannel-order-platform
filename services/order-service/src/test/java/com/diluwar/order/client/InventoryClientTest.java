package com.diluwar.order.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diluwar.order.exception.InventoryReservationException;

class InventoryClientTest {

    private InventoryFeignClient inventoryFeignClient;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        inventoryFeignClient = mock(InventoryFeignClient.class);
        inventoryClient = new InventoryClient(inventoryFeignClient);
    }

    @Test
    void shouldInvokeReserveFallbackGracefully() {
        Throwable cause = new RuntimeException("Inventory service down");

        InventoryReservationException exception = assertThrows(
                InventoryReservationException.class,
                () -> inventoryClient.reserveFallback(1L, 5, cause)
        );

        assertNotNull(exception.getMessage());
    }

    @Test
    void shouldInvokeReleaseFallbackGracefully() {
        Throwable cause = new RuntimeException("Read timeout");

        InventoryReservationException exception = assertThrows(
                InventoryReservationException.class,
                () -> inventoryClient.releaseFallback(1L, 5, cause)
        );

        assertNotNull(exception.getMessage());
    }
}
