package com.diluwar.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity cannot be negative")
        Integer quantity
) {
}
