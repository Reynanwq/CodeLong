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
                .title(API_TITLE)
                .version(API_VERSION)
                .description(API_DESCRIPTION)
        )
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme(BEARER_SCHEME)
                    .bearerFormat(BEARER_FORMAT)
            )
        )
        .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME))

    private companion object {
        const val SECURITY_SCHEME = "bearerAuth"
        const val API_TITLE = "CodeLong API"
        const val API_VERSION = "v1"
        const val API_DESCRIPTION = "Backend do jogo de perguntas sobre programacao e Engenharia de Software."
        const val BEARER_SCHEME = "bearer"
        const val BEARER_FORMAT = "JWT"
    }
}