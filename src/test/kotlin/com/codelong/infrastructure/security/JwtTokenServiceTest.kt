package com.codelong.infrastructure.security

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.TokenClaims
import com.codelong.domain.valueobject.UserId
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

class JwtTokenServiceTest {

    private companion object {
        const val SECRET = "codelong-test-secret-key-with-at-least-32-bytes"
        const val OTHER_SECRET = "outro-secret-key-com-tambem-32-bytes-aqui-123"
        const val ROLE_CLAIM = "role"
    }

    private val properties = SecurityProperties(
        jwt = SecurityProperties.Jwt(secret = SECRET, expiration = Duration.ofHours(2))
    )

    private val service = JwtTokenService(properties)

    private val key = Keys.hmacShaKeyFor(SECRET.toByteArray(StandardCharsets.UTF_8))

    private fun claims(
        userId: String = "u-1",
        role: Role = Role.USER,
        issuedAt: Instant = Instant.parse("2030-01-01T12:00:00Z"),
        expiresAt: Instant = Instant.parse("2030-01-01T14:00:00Z")
    ) = TokenClaims(UserId(userId), role, issuedAt, expiresAt)

    @Test
    fun `gera e le de volta as claims`() {
        val parsed = service.parse(service.generate(claims()))

        assertEquals("u-1", parsed.userId.value)
        assertEquals(Role.USER, parsed.role)
        assertEquals(Instant.parse("2030-01-01T12:00:00Z"), parsed.issuedAt)
        assertEquals(Instant.parse("2030-01-01T14:00:00Z"), parsed.expiresAt)
    }

    @Test
    fun `preserva o papel ADMIN`() {
        val parsed = service.parse(service.generate(claims(role = Role.ADMIN)))

        assertEquals(Role.ADMIN, parsed.role)
    }

    @Test
    fun `preserva identificadores variados`() {
        val parsed = service.parse(service.generate(claims(userId = "user-abc-123")))

        assertEquals("user-abc-123", parsed.userId.value)
    }

    @Test
    fun `token gerado tem tres partes`() {
        val token = service.generate(claims())

        assertEquals(3, token.split(".").size)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `tokens diferentes para claims diferentes`() {
        val first = service.generate(claims(userId = "u-1"))
        val second = service.generate(claims(userId = "u-2"))

        assertTrue(first != second)
    }

    @Test
    fun `rejeita token assinado com outra chave`() {
        val otherService = JwtTokenService(
            SecurityProperties(jwt = SecurityProperties.Jwt(secret = OTHER_SECRET))
        )
        val token = otherService.generate(claims())

        assertThrows<JwtException> { service.parse(token) }
    }

    @Test
    fun `rejeita token malformado`() {
        assertThrows<JwtException> { service.parse("isto-nao-e-um-jwt") }
    }

    @Test
    fun `rejeita token vazio`() {
        assertThrows<IllegalArgumentException> { service.parse("") }
    }

    @Test
    fun `rejeita token expirado`() {
        val expired = claims(
            issuedAt = Instant.parse("2020-01-01T00:00:00Z"),
            expiresAt = Instant.parse("2020-01-01T01:00:00Z")
        )

        assertThrows<JwtException> { service.parse(service.generate(expired)) }
    }

    @Test
    fun `rejeita token sem subject`() {
        val token = Jwts.builder()
            .claim(ROLE_CLAIM, "USER")
            .signWith(key)
            .compact()

        val error = assertThrows<DomainException> { service.parse(token) }

        assertEquals("TOKEN_INVALID", error.code)
        assertEquals("The token is missing its subject", error.message)
    }

    @Test
    fun `rejeita token sem papel`() {
        val token = Jwts.builder()
            .subject("u-1")
            .signWith(key)
            .compact()

        val error = assertThrows<DomainException> { service.parse(token) }

        assertEquals("TOKEN_INVALID", error.code)
        assertEquals("The token is missing its role", error.message)
    }

    @Test
    fun `rejeita papel desconhecido`() {
        val token = Jwts.builder()
            .subject("u-1")
            .claim(ROLE_CLAIM, "SUPERUSER")
            .signWith(key)
            .compact()

        val error = assertThrows<DomainException> { service.parse(token) }

        assertEquals("role.invalid", error.code)
    }

    @Test
    fun `datas ausentes viram epoch`() {
        val token = Jwts.builder()
            .subject("u-1")
            .claim(ROLE_CLAIM, "USER")
            .signWith(key)
            .compact()

        val parsed = service.parse(token)

        assertEquals(Instant.EPOCH, parsed.issuedAt)
        assertEquals(Instant.EPOCH, parsed.expiresAt)
    }

    @Test
    fun `secret em branco falha ao assinar`() {
        val blank = JwtTokenService(SecurityProperties(jwt = SecurityProperties.Jwt(secret = "")))

        val error = assertThrows<IllegalStateException> { blank.generate(claims()) }

        assertTrue(error.message!!.contains("CODELONG_JWT_SECRET"))
    }

    @Test
    fun `secret curto demais falha ao assinar`() {
        val short = JwtTokenService(SecurityProperties(jwt = SecurityProperties.Jwt(secret = "curto")))

        val error = assertThrows<IllegalStateException> { short.generate(claims()) }

        assertTrue(error.message!!.contains("at least 32 bytes"))
    }

    @Test
    fun `secret com exatamente 32 bytes e aceito`() {
        val exact = JwtTokenService(
            SecurityProperties(jwt = SecurityProperties.Jwt(secret = "x".repeat(32)))
        )

        assertEquals("u-1", exact.parse(exact.generate(claims())).userId.value)
    }
}
