package com.lastkey.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME =
            "Bearer Authentication";

    @Bean
    public OpenAPI lastKeyOpenApi() {

        SecurityScheme bearerSecurityScheme =
                new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description(
                                """
                                Enter your JWT access token.

                                Do not add the word Bearer manually.
                                Swagger will automatically send:

                                Authorization: Bearer YOUR_TOKEN
                                """
                        );

        SecurityRequirement globalSecurityRequirement =
                new SecurityRequirement()
                        .addList(
                                SECURITY_SCHEME_NAME
                        );

        Contact contact =
                new Contact()
                        .name("LastKey Development Team")
                        .email("support@lastkey.com");

        License license =
                new License()
                        .name("Private Project");

        Info apiInfo =
                new Info()
                        .title("LastKey Backend API")
                        .version("1.0.0")
                        .description(
                                """
                                LastKey is an AI-powered digital legacy
                                management platform.

                                Main features:

                                - User authentication and authorization
                                - JWT access and refresh tokens
                                - Secure document management
                                - AES encrypted file storage
                                - OCR document scanning
                                - AI document analysis
                                - Category management
                                - Nominee management
                                - Emergency request workflow
                                - Automatic document release
                                - Nominee secure document access
                                - Notifications
                                - Dashboard and analytics

                                Authentication:

                                Most APIs require a valid JWT access token.

                                Use the Authorize button in Swagger UI and
                                paste only the access token. Do not manually
                                add the Bearer prefix.
                                """
                        )
                        .contact(contact)
                        .license(license);

        Server localServer =
                new Server()
                        .url("http://localhost:8080")
                        .description(
                                "Local development server"
                        );

        return new OpenAPI()
                .info(apiInfo)
                .servers(
                        List.of(localServer)
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        bearerSecurityScheme
                                )
                )
                .addSecurityItem(
                        globalSecurityRequirement
                );
    }
}