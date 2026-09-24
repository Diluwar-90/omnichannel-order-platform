package com.diluwar.payment.integration;

import com.diluwar.payment.dto.PaymentResponse;
import com.diluwar.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:application-test.yaml"
)
class PaymentIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldCreatePaymentThroughFullStack() {

        String request = """
                {
                    "orderId": 9001,
                    "amount": 1999.99,
                    "currency": "INR"
                }
                """;

        PaymentResponse response =
                restClient()
                        .post()
                        .uri("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(PaymentResponse.class);

        assertNotNull(response);

        assertEquals(
                9001L,
                response.getOrderId()
        );

        assertEquals(
                new BigDecimal("1999.99"),
                response.getAmount()
        );

        assertEquals(
                "INR",
                response.getCurrency()
        );

        assertEquals(
                PaymentStatus.PENDING,
                response.getStatus()
        );
    }

    @Test
    void shouldGetPaymentByOrderIdThroughFullStack() {

        String request = """
                {
                    "orderId": 9002,
                    "amount": 2500.00,
                    "currency": "INR"
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        PaymentResponse response =
                restClient()
                        .get()
                        .uri("/api/v1/payments/order/9002")
                        .retrieve()
                        .body(PaymentResponse.class);

        assertNotNull(response);

        assertEquals(
                9002L,
                response.getOrderId()
        );

        assertEquals(
                new BigDecimal("2500.00"),
                response.getAmount()
        );

        assertEquals(
                PaymentStatus.PENDING,
                response.getStatus()
        );
    }

    @Test
    void shouldAuthorizePaymentThroughFullStack() {

        String createRequest = """
                {
                    "orderId": 9003,
                    "amount": 5000.00,
                    "currency": "INR"
                }
                """;

        PaymentResponse created =
                restClient()
                        .post()
                        .uri("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createRequest)
                        .retrieve()
                        .body(PaymentResponse.class);

        assertNotNull(created);

        Long paymentId = created.getId();

        String statusRequest = """
                {
                    "status": "AUTHORIZED"
                }
                """;

        PaymentResponse response =
                restClient()
                        .patch()
                        .uri("/api/v1/payments/" + paymentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(statusRequest)
                        .retrieve()
                        .body(PaymentResponse.class);

        assertNotNull(response);

        assertEquals(
                PaymentStatus.AUTHORIZED,
                response.getStatus()
        );
    }
}