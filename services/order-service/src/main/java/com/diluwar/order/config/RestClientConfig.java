package com.diluwar.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient inventoryRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }

    @Bean
    RestClient paymentRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8085")
                .build();
    }
}