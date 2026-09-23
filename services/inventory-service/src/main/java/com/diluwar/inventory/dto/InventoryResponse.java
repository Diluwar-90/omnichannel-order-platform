package com.diluwar.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
