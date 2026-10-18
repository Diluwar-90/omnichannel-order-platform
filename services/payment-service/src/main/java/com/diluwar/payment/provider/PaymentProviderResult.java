package com.diluwar.payment.provider;

public record PaymentProviderResult(
        boolean success,
        String transactionId,
        String message
) {
    public static PaymentProviderResult success(String transactionId) {
        return new PaymentProviderResult(true, transactionId, "Payment processed successfully");
    }

    public static PaymentProviderResult failure(String message) {
        return new PaymentProviderResult(false, null, message);
    }
}
