package com.diluwar.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient inventoryRestClient(
            @Value("${INVENTORY_SERVICE_URL:http://localhost:8083}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    RestClient paymentRestClient(
            @Value("${PAYMENT_SERVICE_URL:http://localhost:8085}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
