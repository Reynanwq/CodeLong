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
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required")
        }

    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access denied")
        }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }

    private companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ADMIN_ENDPOINTS = "/api/admin/**"
    }

    private fun publicEndpoints(): Array<String> = arrayOf(
        "/api/auth/**",
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    )
}