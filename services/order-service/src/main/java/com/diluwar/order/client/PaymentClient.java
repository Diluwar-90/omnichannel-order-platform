package com.diluwar.order.client;

import com.diluwar.order.dto.PaymentResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

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

    private record CreatePaymentRequest(
            Long orderId,
            BigDecimal amount,
            String currency
    ) {
    }
}
