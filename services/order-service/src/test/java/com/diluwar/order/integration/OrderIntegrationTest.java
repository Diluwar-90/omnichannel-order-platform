package com.diluwar.order.integration;

import com.diluwar.order.dto.OrderResponse;
import com.diluwar.order.entity.OrderStatus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT

)
class OrderIntegrationTest {

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
                    .uri("/api/orders/" + created.id() + "/status?status=CONFIRMED")
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

    var exception = org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () -> restClient()
                    .get()
                    .uri("/api/orders/" + created.id())
                    .retrieve()
                    .toBodilessEntity()
    );

    assertNotNull(exception);
}

}