package com.codelong.infrastructure.security

import com.codelong.domain.exception.Errors


import com.codelong.domain.port.TokenService
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.TokenClaims
import com.codelong.domain.valueobject.UserId
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenService(
    private val properties: SecurityProperties
) : TokenService {

    private val key: SecretKey by lazy { buildKey(properties.jwt.secret) }

    override fun generate(claims: TokenClaims): String =
        Jwts.builder()
            .subject(claims.userIdText())
            .claim(ROLE_CLAIM, claims.roleName())
            .issuedAt(Date.from(claims.issuedAt))
            .expiration(Date.from(claims.expiresAt))
            .signWith(key)
            .compact()

    override fun parse(token: String): TokenClaims {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val subject = claims.subject
            ?: throw Errors.tokenWithoutSubject()
        val role = claims[ROLE_CLAIM] as? String
            ?: throw Errors.tokenWithoutRole()

        return TokenClaims(
            userId = UserId(subject),
            role = Role.fromName(role),
            issuedAt = toInstant(claims.issuedAt),
            expiresAt = toInstant(claims.expiration)
        )
    }

    private fun toInstant(date: Date?): Instant =
        date?.toInstant() ?: Instant.EPOCH

    private fun buildKey(secret: String): SecretKey {
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        check(secret.isNotBlank()) { SECRET_NOT_CONFIGURED }
        check(bytes.size >= MIN_SECRET_BYTES) { SECRET_TOO_SHORT }
        return Keys.hmacShaKeyFor(bytes)
    }

    private companion object {
        const val ROLE_CLAIM = "role"
        const val MIN_SECRET_BYTES = 32
        const val SECRET_NOT_CONFIGURED =
            "codelong.jwt.secret is not configured. Set the CODELONG_JWT_SECRET environment variable."
        const val SECRET_TOO_SHORT =
            "codelong.jwt.secret must have at least $MIN_SECRET_BYTES bytes (UTF-8) to sign HS256 tokens."
    }
}