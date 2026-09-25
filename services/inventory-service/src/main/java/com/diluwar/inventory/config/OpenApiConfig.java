package com.diluwar.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .version("1.0.0")
                        .description(
                                "REST API for inventory management in the "
                                        + "Omnichannel Order Platform"
                        ));
    }
}