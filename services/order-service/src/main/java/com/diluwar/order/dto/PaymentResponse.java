package com.diluwar.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String currency,
        String status,
        String transactionId,
        Instant createdAt,
        Instant updatedAt
) {
}
