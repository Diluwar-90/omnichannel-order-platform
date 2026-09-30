package com.diluwar.order.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String currency,
        Instant updatedAt
) {
}
