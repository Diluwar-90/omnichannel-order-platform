package com.diluwar.payment.exception;

public class InvalidPaymentStatusException extends RuntimeException {

    public InvalidPaymentStatusException(
            String currentStatus,
            String requestedStatus) {

        super("Invalid payment status transition from "
                + currentStatus
                + " to "
                + requestedStatus);
    }
}