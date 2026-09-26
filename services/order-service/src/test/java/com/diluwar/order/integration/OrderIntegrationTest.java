package com.diluwar.order.integration;

import com.diluwar.order.dto.OrderResponse;
import com.diluwar.order.dto.PaymentResponse;
import com.diluwar.order.entity.OrderStatus;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderIntegrationTest {

    private static final Network NETWORK =
            Network.newNetwork();

    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17")
                .withNetwork(NETWORK)
                .withNetworkAliases("postgres")
                .withDatabaseName("order_db")
                .withUsername("app_user")
                .withPassword("app_password")
                .withInitScript("init-test-databases.sql");

    static GenericContainer<?> inventoryService =
            new GenericContainer<>("inventory-service:test")
                    .withNetwork(NETWORK)
                    .withExposedPorts(8083)
                    .withEnv(
                            "DB_URL",
                            "jdbc:postgresql://postgres:5432/inventory_db"
                    )
                    .withEnv("DB_USERNAME", "app_user")
                    .withEnv("DB_PASSWORD", "app_password")
                    .withEnv("SERVER_PORT", "8083")
                    .dependsOn(postgres);



    static GenericContainer<?> paymentService =
            new GenericContainer<>("payment-service:test")
                    .withNetwork(NETWORK)
                    .withExposedPorts(8085)
                    .withEnv(
                            "DB_URL",
                            "jdbc:postgresql://postgres:5432/payment_db"
                    )
                    .withEnv("DB_USERNAME", "app_user")
                    .withEnv("DB_PASSWORD", "app_password")
                    .withEnv("SERVER_PORT", "8085")
                    .dependsOn(postgres);

    static {
    postgres.start();

    inventoryService.start();
    System.out.println("=== INVENTORY MAPPED PORT: " +
    inventoryService.getMappedPort(8083) + " ===");

    System.out.println("=== INVENTORY CONTAINER LOGS ===");
    System.out.println(inventoryService.getLogs());
    System.out.println("=== END INVENTORY LOGS ===");

   paymentService.start();
}

//     static {
//         postgres.start();
//         inventoryService.start();
//         paymentService.start();
//     }

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "INVENTORY_SERVICE_URL",
                () -> "http://localhost:" +
                        inventoryService.getMappedPort(8083)
        );

        registry.add(
                "PAYMENT_SERVICE_URL",
                () -> "http://localhost:" +
                        paymentService.getMappedPort(8085)
        );
    }

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldCreateOrderThroughFullStack() {

        String request = """
                {
                    "customerId": 1001,
                    "items": [
                        {
                            "productId": 501,
                            "quantity": 2,
                            "unitPrice": 499.99
                        }
                    ]
                }
                """;

        OrderResponse response =
                restClient()
                        .post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(response);
        assertNotNull(response.id());

        assertEquals(
                1001L,
                response.customerId()
        );

        assertNotNull(response.status());

        assertEquals(
                new BigDecimal("999.98"),
                response.totalAmount()
        );

        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        assertNotNull(response.items());
        assertEquals(1, response.items().size());
    }

    @Test
    void shouldGetOrderByIdThroughFullStack() {

        String request = """
                {
                    "customerId": 1002,
                    "items": [
                        {
                            "productId": 502,
                            "quantity": 3,
                            "unitPrice": 250.00
                        }
                    ]
                }
                """;

        OrderResponse created =
                restClient()
                        .post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(created);
        assertNotNull(created.id());

        OrderResponse response =
                restClient()
                        .get()
                        .uri("/api/orders/" + created.id())
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(response);

        assertEquals(
                created.id(),
                response.id()
        );

        assertEquals(
                1002L,
                response.customerId()
        );

        assertEquals(
                new BigDecimal("750.00"),
                response.totalAmount()
        );

        assertNotNull(response.items());
        assertEquals(1, response.items().size());
    }

    @Test
    void shouldUpdateOrderStatusThroughFullStack() {

        String request = """
                {
                    "customerId": 1003,
                    "items": [
                        {
                            "productId": 503,
                            "quantity": 1,
                            "unitPrice": 1200.00
                        }
                    ]
                }
                """;

        OrderResponse created =
                restClient()
                        .post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(created);
        assertNotNull(created.id());

        OrderResponse response =
                restClient()
                        .put()
                        .uri(
                                "/api/orders/" +
                                created.id() +
                                "/status?status=CONFIRMED"
                        )
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(response);

        assertEquals(
                created.id(),
                response.id()
        );

        assertEquals(
                1003L,
                response.customerId()
        );

        assertEquals(
                OrderStatus.CONFIRMED,
                response.status()
        );

        assertEquals(
                new BigDecimal("1200.00"),
                response.totalAmount()
        );
    }

    @Test
    void shouldGetOrdersWithPaginationThroughFullStack() {

        String request1 = """
                {
                    "customerId": 2001,
                    "items": [
                        {
                            "productId": 601,
                            "quantity": 1,
                            "unitPrice": 100.00
                        }
                    ]
                }
                """;

        String request2 = """
                {
                    "customerId": 2002,
                    "items": [
                        {
                            "productId": 602,
                            "quantity": 2,
                            "unitPrice": 200.00
                        }
                    ]
                }
                """;

        restClient()
                .post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .retrieve()
                .toBodilessEntity();

        restClient()
                .post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve()
                .toBodilessEntity();

        String response =
                restClient()
                        .get()
                        .uri("/api/orders?page=0&size=10")
                        .retrieve()
                        .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("\"content\""));
        assertTrue(response.contains("\"totalElements\""));
        assertTrue(response.contains("\"totalPages\""));
    }

    @Test
    void shouldDeleteOrderThroughFullStack() {

        String request = """
                {
                    "customerId": 3001,
                    "items": [
                        {
                            "productId": 701,
                            "quantity": 1,
                            "unitPrice": 500.00
                        }
                    ]
                }
                """;

        OrderResponse created =
                restClient()
                        .post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OrderResponse.class);

        assertNotNull(created);
        assertNotNull(created.id());

        restClient()
                .delete()
                .uri("/api/orders/" + created.id())
                .retrieve()
                .toBodilessEntity();

        var exception = assertThrows(
                Exception.class,
                () -> restClient()
                        .get()
                        .uri("/api/orders/" + created.id())
                        .retrieve()
                        .toBodilessEntity()
        );

        assertNotNull(exception);
    }

    @Test
void shouldGetPaymentByOrderIdThroughFullStack() {

    String request = """
            {
                "customerId": 4001,
                "items": [
                    {
                        "productId": 801,
                        "quantity": 2,
                        "unitPrice": 250.00
                    }
                ]
            }
            """;

    OrderResponse created =
            restClient()
                    .post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(created);
    assertNotNull(created.id());

    PaymentResponse payment =
            restClient()
                    .get()
                    .uri("/api/orders/" + created.id() + "/payment")
                    .retrieve()
                    .body(PaymentResponse.class);

    assertNotNull(payment);
    assertEquals(created.id(), payment.orderId());
    assertEquals("PENDING", payment.status());
    assertEquals(
            new BigDecimal("500.00"),
            payment.amount()
    );
    assertEquals("INR", payment.currency());
}

@Test
void shouldConfirmOrderWhenPaymentIsCapturedThroughFullStack() {

    String request = """
            {
                "customerId": 5001,
                "items": [
                    {
                        "productId": 501,
                        "quantity": 2,
                        "unitPrice": 300.00
                    }
                ]
            }
            """;

    OrderResponse created =
            restClient()
                    .post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(created);
    assertNotNull(created.id());

    PaymentResponse payment =
            restClient()
                    .get()
                    .uri("/api/orders/" + created.id() + "/payment")
                    .retrieve()
                    .body(PaymentResponse.class);

    assertNotNull(payment);
    assertEquals(created.id(), payment.orderId());
    assertEquals("PENDING", payment.status());

    PaymentResponse authorizedPayment =
        restClient()
                .patch()
                .uri(
                        "http://localhost:" +
                        paymentService.getMappedPort(8085) +
                        "/api/v1/payments/" +
                        payment.id() +
                        "/status"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "status": "AUTHORIZED"
                        }
                        """)
                .retrieve()
                .body(PaymentResponse.class);

assertNotNull(authorizedPayment);
assertEquals("AUTHORIZED", authorizedPayment.status());

PaymentResponse capturedPayment =
        restClient()
                .patch()
                .uri(
                        "http://localhost:" +
                        paymentService.getMappedPort(8085) +
                        "/api/v1/payments/" +
                        payment.id() +
                        "/status"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "status": "CAPTURED"
                        }
                        """)
                .retrieve()
                .body(PaymentResponse.class);

assertNotNull(capturedPayment);
assertEquals("CAPTURED", capturedPayment.status());

    OrderResponse confirmed =
            restClient()
                    .put()
                    .uri("/api/orders/" + created.id() + "/confirm")
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(confirmed);
    assertEquals(created.id(), confirmed.id());
    assertEquals(
            OrderStatus.CONFIRMED,
            confirmed.status()
    );
}

@Test
void shouldConfirmOrderThroughFullStack() {

    String request = """
            {
                "customerId": 5001,
                "items": [
                    {
                        "productId": 801,
                        "quantity": 1,
                        "unitPrice": 500.00
                    }
                ]
            }
            """;

    OrderResponse created =
            restClient()
                    .post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(created);
    assertNotNull(created.id());

    assertEquals(
            OrderStatus.CREATED,
            created.status()
    );

    // Payment service
    RestClient paymentClient =
            RestClient.builder()
                    .baseUrl(
                            "http://localhost:" +
                            paymentService.getMappedPort(8085)
                    )
                    .build();

    // PENDING
    PaymentResponse payment =
            paymentClient
                    .get()
                    .uri("/api/v1/payments/order/" + created.id())
                    .retrieve()
                    .body(PaymentResponse.class);

    assertNotNull(payment);
    assertEquals("PENDING", payment.status());

    // PENDING -> AUTHORIZED
    paymentClient
            .patch()
            .uri("/api/v1/payments/" + payment.id() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                    {
                        "status": "AUTHORIZED"
                    }
                    """)
            .retrieve()
            .body(PaymentResponse.class);

    // AUTHORIZED -> CAPTURED
    paymentClient
            .patch()
            .uri("/api/v1/payments/" + payment.id() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                    {
                        "status": "CAPTURED"
                    }
                    """)
            .retrieve()
            .body(PaymentResponse.class);

    // Now order can be confirmed
    OrderResponse confirmed =
            restClient()
                    .put()
                    .uri("/api/orders/" + created.id() + "/confirm")
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(confirmed);

    assertEquals(
            created.id(),
            confirmed.id()
    );

    assertEquals(
            OrderStatus.CONFIRMED,
            confirmed.status()
    );
}

@Test
void shouldNotConfirmOrderWhenPaymentIsNotCaptured() {

    String request = """
            {
                "customerId": 5002,
                "items": [
                    {
                        "productId": 801,
                        "quantity": 1,
                        "unitPrice": 600.00
                    }
                ]
            }
            """;

    OrderResponse created =
            restClient()
                    .post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);

    assertNotNull(created);
    assertEquals(
            OrderStatus.CREATED,
            created.status()
    );

    // Payment remains PENDING

            // Payment remains PENDING

    var exception = assertThrows(
        org.springframework.web.client.HttpClientErrorException.Conflict.class,
        () -> restClient()
                .put()
                .uri("/api/orders/" + created.id() + "/confirm")
                .retrieve()
                .toBodilessEntity()
        );

        assertEquals(409, exception.getStatusCode().value());
}

}
