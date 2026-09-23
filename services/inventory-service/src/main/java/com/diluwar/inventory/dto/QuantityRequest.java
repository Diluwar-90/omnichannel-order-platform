package com.diluwar.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuantityRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
