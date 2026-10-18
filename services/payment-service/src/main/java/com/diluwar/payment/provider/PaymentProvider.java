package com.diluwar.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    PaymentProviderResult processPayment(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    );
}
