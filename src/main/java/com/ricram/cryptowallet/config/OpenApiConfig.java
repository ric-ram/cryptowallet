package com.ricram.cryptowallet.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Wallet Management")
                        .version("1.0.0")
                        .description("Manage wallets, assets, valuations and profit simulations")
                        .contact(new Contact()
                                .name("Ricardo Ramos")
                                .email("r.ramos@ricardoframos.com")
                        )
                );
    }
}
