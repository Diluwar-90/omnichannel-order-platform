package com.diluwar.notification.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCapturedEvent(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String currency,
        String transactionId,
        Instant updatedAt
) {
}