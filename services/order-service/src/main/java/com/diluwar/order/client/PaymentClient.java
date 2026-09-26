package com.diluwar.order.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.diluwar.order.dto.PaymentResponse;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(
            @Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public PaymentResponse createPayment(
            Long orderId,
            BigDecimal amount,
            String currency) {

        return restClient
                .post()
                .uri("/api/v1/payments")
                .body(new CreatePaymentRequest(
                        orderId,
                        amount,
                        currency
                ))
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {

        return restClient
                .get()
                .uri("/api/v1/payments/order/" + orderId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    private record CreatePaymentRequest(
            Long orderId,
            BigDecimal amount,
            String currency
    ) {
    }
}