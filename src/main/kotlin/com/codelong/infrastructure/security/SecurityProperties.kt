package com.codelong.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val PROPERTIES_PREFIX = "codelong"

@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
data class SecurityProperties(
    val jwt: Jwt = Jwt(),
    val cors: Cors = Cors(),
    val admin: Admin = Admin()
) {
    data class Jwt(
        val secret: String = "",
        val expiration: Duration = Duration.ofHours(8)
    )

    data class Cors(
        val allowedOrigins: List<String> = emptyList()
    )

    data class Admin(
        val username: String = "",
        val password: String = ""
    )
}