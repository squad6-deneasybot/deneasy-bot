package com.squad6.deneasybot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("DeneasyBot API")
                .version("1.0")
                .description("Documentação da API do Assistente Virtual DeneasyBot")
                .contact(new Contact()
                        .name("Squad 6")
                        .email("contact@example.com"))
                .license(new License()
                        .name("License Name")
                        .url("https://example.com/license"))
        );
    }
}