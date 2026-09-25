package com.diluwar.product.integration;

import com.diluwar.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ProductIntegrationTest {

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
    void shouldCreateProductThroughFullStack() {

        String request = """
                {
                    "name": "iPhone 17",
                    "description": "Apple smartphone",
                    "price": 79999.00,
                    "stockQuantity": 25
                }
                """;

        ProductResponse response =
                restClient()
                        .post()
                        .uri("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(response);
        assertNotNull(response.getId());

        assertEquals(
                "iPhone 17",
                response.getName()
        );

        assertEquals(
                "Apple smartphone",
                response.getDescription()
        );

        assertEquals(
                79999.00,
                response.getPrice()
        );

        assertEquals(
                25,
                response.getStockQuantity()
        );
    }

    @Test
    void shouldGetProductByIdThroughFullStack() {

        String request = """
                {
                    "name": "MacBook Pro",
                    "description": "Apple laptop",
                    "price": 149999.00,
                    "stockQuantity": 10
                }
                """;

        ProductResponse created =
                restClient()
                        .post()
                        .uri("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(created);
        assertNotNull(created.getId());

        ProductResponse response =
                restClient()
                        .get()
                        .uri("/api/products/" + created.getId())
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(response);

        assertEquals(
                created.getId(),
                response.getId()
        );

        assertEquals(
                "MacBook Pro",
                response.getName()
        );

        assertEquals(
                149999.00,
                response.getPrice()
        );

        assertEquals(
                10,
                response.getStockQuantity()
        );
    }

    @Test
    void shouldUpdateProductThroughFullStack() {

        String createRequest = """
                {
                    "name": "Samsung Galaxy",
                    "description": "Samsung smartphone",
                    "price": 59999.00,
                    "stockQuantity": 20
                }
                """;

        ProductResponse created =
                restClient()
                        .post()
                        .uri("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createRequest)
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(created);

        String updateRequest = """
                {
                    "name": "Samsung Galaxy S26",
                    "description": "Updated smartphone",
                    "price": 69999.00,
                    "stockQuantity": 30
                }
                """;

        ProductResponse updated =
                restClient()
                        .put()
                        .uri("/api/products/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(updateRequest)
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(updated);

        assertEquals(
                created.getId(),
                updated.getId()
        );

        assertEquals(
                "Samsung Galaxy S26",
                updated.getName()
        );

        assertEquals(
                "Updated smartphone",
                updated.getDescription()
        );

        assertEquals(
                69999.00,
                updated.getPrice()
        );

        assertEquals(
                30,
                updated.getStockQuantity()
        );
    }

    @Test
    void shouldDeleteProductThroughFullStack() {

        String request = """
                {
                    "name": "Temporary Product",
                    "description": "Product for delete test",
                    "price": 1000.00,
                    "stockQuantity": 5
                }
                """;

        ProductResponse created =
                restClient()
                        .post()
                        .uri("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(ProductResponse.class);

        assertNotNull(created);

        restClient()
                .delete()
                .uri("/api/products/" + created.getId())
                .retrieve()
                .toBodilessEntity();

       HttpClientErrorException.NotFound exception =
        assertThrows(
                HttpClientErrorException.NotFound.class,
                () -> restClient()
                        .get()
                        .uri("/api/products/" + created.getId())
                        .retrieve()
                        .body(ProductResponse.class)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            exception.getStatusCode()
    );
    }

    @Test
    void shouldGetAllProductsThroughFullStack() {

        String request = """
                {
                    "name": "Test Product",
                    "description": "Integration test product",
                    "price": 1999.00,
                    "stockQuantity": 15
                }
                """;

        restClient()
                .post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        ProductResponse[] products =
                restClient()
                        .get()
                        .uri("/api/products")
                        .retrieve()
                        .body(ProductResponse[].class);

        assertNotNull(products);
        assertTrue(products.length > 0);
    }

    @Test
void shouldRejectProductWhenNameIsMissing() {

    String request = """
            {
                "description": "Invalid product",
                "price": 1000.00,
                "stockQuantity": 10
            }
            """;

    HttpClientErrorException.BadRequest exception =
            assertThrows(
                    HttpClientErrorException.BadRequest.class,
                    () -> restClient()
                            .post()
                            .uri("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(ProductResponse.class)
            );

    assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
    );
}

@Test
void shouldRejectProductWhenPriceIsZero() {

    String request = """
            {
                "name": "Invalid Product",
                "description": "Invalid price",
                "price": 0,
                "stockQuantity": 10
            }
            """;

    HttpClientErrorException.BadRequest exception =
            assertThrows(
                    HttpClientErrorException.BadRequest.class,
                    () -> restClient()
                            .post()
                            .uri("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(ProductResponse.class)
            );

    assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
    );
}
@Test
void shouldRejectProductWhenStockIsNegative() {

    String request = """
            {
                "name": "Invalid Product",
                "description": "Invalid stock",
                "price": 1000.00,
                "stockQuantity": -1
            }
            """;

    HttpClientErrorException.BadRequest exception =
            assertThrows(
                    HttpClientErrorException.BadRequest.class,
                    () -> restClient()
                            .post()
                            .uri("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(ProductResponse.class)
            );

    assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
    );
}



}