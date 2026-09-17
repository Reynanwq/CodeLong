package com.codelong.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun codelongOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("CodeLong API")
                .version("v1")
                .description("Backend do jogo de perguntas sobre programacao e Engenharia de Software.")
        )
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        )
        .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME))

    private companion object {
        const val SECURITY_SCHEME = "bearerAuth"
    }
}