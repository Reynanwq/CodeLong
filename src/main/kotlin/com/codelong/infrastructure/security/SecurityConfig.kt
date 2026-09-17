package com.codelong.infrastructure.security

import com.codelong.domain.port.TokenService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig(
    private val tokenService: TokenService,
    private val properties: SecurityProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(*publicEndpoints()).permitAll()
                auth.requestMatchers(ADMIN_ENDPOINTS).hasRole(ROLE_ADMIN)
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint())
                ex.accessDeniedHandler(accessDeniedHandler())
            }
            .addFilterBefore(JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = properties.cors.allowedOrigins
            allowedMethods = allowedMethods()
            allowedHeaders = listOf(ANY_HEADER)
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration(ALL_PATHS, configuration)
        return source
    }

    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE)
        }

    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            writeError(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_CODE, FORBIDDEN_MESSAGE)
        }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(ERROR_BODY_TEMPLATE.format(code, message))
    }

    private companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ADMIN_ENDPOINTS = "/api/admin/**"
        const val ALL_PATHS = "/**"
        const val ANY_HEADER = "*"
        const val UNAUTHORIZED_CODE = "UNAUTHORIZED"
        const val UNAUTHORIZED_MESSAGE = "Authentication required"
        const val FORBIDDEN_CODE = "FORBIDDEN"
        const val FORBIDDEN_MESSAGE = "Access denied"
        const val ERROR_BODY_TEMPLATE = """{"code":"%s","message":"%s"}"""
        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"
        const val METHOD_PUT = "PUT"
        const val METHOD_PATCH = "PATCH"
        const val METHOD_DELETE = "DELETE"
        const val METHOD_OPTIONS = "OPTIONS"

        fun allowedMethods(): List<String> = listOf(
            METHOD_GET,
            METHOD_POST,
            METHOD_PUT,
            METHOD_PATCH,
            METHOD_DELETE,
            METHOD_OPTIONS
        )

        fun publicEndpoints(): Array<String> = arrayOf(
            AUTH_ENDPOINTS,
            HEALTH_ENDPOINT,
            INFO_ENDPOINT,
            API_DOCS_ENDPOINTS,
            SWAGGER_UI_ENDPOINTS,
            SWAGGER_UI_HTML
        )

        const val AUTH_ENDPOINTS = "/api/auth/**"
        const val HEALTH_ENDPOINT = "/actuator/health"
        const val INFO_ENDPOINT = "/actuator/info"
        const val API_DOCS_ENDPOINTS = "/v3/api-docs/**"
        const val SWAGGER_UI_ENDPOINTS = "/swagger-ui/**"
        const val SWAGGER_UI_HTML = "/swagger-ui.html"
    }
}