package com.diluwar.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "customerId is required")
        Long customerId,

        @NotEmpty(message = "items cannot be empty")
        List<@Valid OrderItemRequest> items
) {
}
