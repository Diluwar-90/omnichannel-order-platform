package com.diluwar.order.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * High-performance HTTP client configuration engineered for distributed systems resilience.
 * <p>
 * Key Design Principles:
 * - Thread Pool Isolation & Non-blocking Sockets: Employs modern Java 21 HTTP Client via JdkClientHttpRequestFactory.
 * - Strict Timeout Boundaries: Configures explicit connect (3s) and read (5s) thresholds to prevent
 *   indefinite thread hangs and TCP socket exhaustion during downstream network partitions.
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private JdkClientHttpRequestFactory createRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Bean
    RestClient inventoryRestClient(
            @Value("${INVENTORY_SERVICE_URL:http://localhost:8083}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .build();
    }

    @Bean
    RestClient paymentRestClient(
            @Value("${PAYMENT_SERVICE_URL:http://localhost:8085}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .build();
    }
}
