package com.diluwar.inventory.integration;

import com.diluwar.inventory.dto.InventoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class InventoryIntegrationTest {

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
    void shouldCreateInventoryThroughFullStack() {

        String request = """
                {
                    "productId": 1001,
                    "quantity": 50
                }
                """;

        InventoryResponse response =
                restClient()
                        .post()
                        .uri("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(response);

        assertNotNull(response.id());

        assertEquals(
                1001L,
                response.productId()
        );

        assertEquals(
                50,
                response.quantity()
        );

        assertEquals(
                0,
                response.reservedQuantity()
        );

        assertEquals(
                50,
                response.availableQuantity()
        );
    }

    @Test
    void shouldGetInventoryThroughFullStack() {

        String request = """
                {
                    "productId": 1002,
                    "quantity": 100
                }
                """;

        InventoryResponse created =
                restClient()
                        .post()
                        .uri("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(created);

        InventoryResponse response =
                restClient()
                        .get()
                        .uri("/api/v1/inventory/1002")
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(response);

        assertEquals(
                created.id(),
                response.id()
        );

        assertEquals(
                1002L,
                response.productId()
        );

        assertEquals(
                100,
                response.quantity()
        );

        assertEquals(
                0,
                response.reservedQuantity()
        );

        assertEquals(
                100,
                response.availableQuantity()
        );
    }

    @Test
    void shouldReserveInventoryThroughFullStack() {

        String createRequest = """
                {
                    "productId": 1003,
                    "quantity": 100
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String reserveRequest = """
                {
                    "quantity": 30
                }
                """;

        InventoryResponse response =
                restClient()
                        .post()
                        .uri("/api/v1/inventory/1003/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reserveRequest)
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(response);

        assertEquals(
                100,
                response.quantity()
        );

        assertEquals(
                30,
                response.reservedQuantity()
        );

        assertEquals(
                70,
                response.availableQuantity()
        );
    }

    @Test
    void shouldReleaseReservedInventoryThroughFullStack() {

        String createRequest = """
                {
                    "productId": 1004,
                    "quantity": 100
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String reserveRequest = """
                {
                    "quantity": 40
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory/1004/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reserveRequest)
                .retrieve()
                .toBodilessEntity();

        String releaseRequest = """
                {
                    "quantity": 15
                }
                """;

        InventoryResponse response =
                restClient()
                        .post()
                        .uri("/api/v1/inventory/1004/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(releaseRequest)
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(response);

        assertEquals(
                100,
                response.quantity()
        );

        assertEquals(
                25,
                response.reservedQuantity()
        );

        assertEquals(
                75,
                response.availableQuantity()
        );
    }

    @Test
    void shouldReduceInventoryThroughFullStack() {

        String createRequest = """
                {
                    "productId": 1005,
                    "quantity": 100
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String reduceRequest = """
                {
                    "quantity": 30
                }
                """;

        InventoryResponse response =
                restClient()
                        .post()
                        .uri("/api/v1/inventory/1005/reduce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reduceRequest)
                        .retrieve()
                        .body(InventoryResponse.class);

        assertNotNull(response);

        assertEquals(
                70,
                response.quantity()
        );

        assertEquals(
                0,
                response.reservedQuantity()
        );

        assertEquals(
                70,
                response.availableQuantity()
        );
    }

    @Test
    void shouldCheckAvailabilityThroughFullStack() {

        String createRequest = """
                {
                    "productId": 1006,
                    "quantity": 50
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String response =
                restClient()
                        .get()
                        .uri("/api/v1/inventory/1006/availability?quantity=30")
                        .retrieve()
                        .body(String.class);

        assertNotNull(response);

        assertTrue(response.contains("\"available\":true"));
        assertTrue(response.contains("\"requestedQuantity\":30"));
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {

        String createRequest = """
                {
                    "productId": 1007,
                    "quantity": 20
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String reserveRequest = """
                {
                    "quantity": 25
                }
                """;

        assertThrows(
                Exception.class,
                () -> restClient()
                        .post()
                        .uri("/api/v1/inventory/1007/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reserveRequest)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    @Test
    void shouldRejectReleaseWhenReservedQuantityIsInsufficient() {

        String createRequest = """
                {
                    "productId": 1008,
                    "quantity": 100
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toBodilessEntity();

        String releaseRequest = """
                {
                    "quantity": 10
                }
                """;

        assertThrows(
                Exception.class,
                () -> restClient()
                        .post()
                        .uri("/api/v1/inventory/1008/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(releaseRequest)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    @Test
    void shouldRejectDuplicateInventoryThroughFullStack() {

        String request = """
                {
                    "productId": 1009,
                    "quantity": 50
                }
                """;

        restClient()
                .post()
                .uri("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThrows(
                Exception.class,
                () -> restClient()
                        .post()
                        .uri("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity()
        );
    }
}