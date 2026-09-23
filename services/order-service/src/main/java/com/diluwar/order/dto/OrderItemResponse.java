package com.diluwar.order.dto;

public record OrderItemResponse(
        Long id,
        Long productId,
        Integer quantity,
        Double unitPrice
) {
}
