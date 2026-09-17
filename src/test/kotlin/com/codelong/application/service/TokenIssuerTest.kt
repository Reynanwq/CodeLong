package com.codelong.application.service

import com.codelong.domain.port.TokenService
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.TokenClaims
import com.codelong.support.Fixtures
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class TokenIssuerTest {

    private class CapturingTokenService : TokenService {
        val claims = mutableListOf<TokenClaims>()

        override fun generate(claims: TokenClaims): String {
            this.claims.add(claims)
            return "token-${claims.userId.value}-${this.claims.size}"
        }

        override fun parse(token: String): TokenClaims = throw UnsupportedOperationException()
    }

    private val tokenService = CapturingTokenService()
    private val expiration = Duration.ofHours(2)
    private val issuer = TokenIssuer(tokenService, TestClock.fixed, expiration)

    @Test
    fun `issue devolve o token gerado pelo servico`() {
        val token = issuer.issue(Fixtures.user(id = "u-1"))

        assertTrue(token.startsWith("token-u-1"))
    }

    @Test
    fun `issue usa o id do usuario nas claims`() {
        issuer.issue(Fixtures.user(id = "u-42"))

        assertEquals("u-42", tokenService.claims.single().userId.value)
    }

    @Test
    fun `issue usa o papel do usuario nas claims`() {
        issuer.issue(Fixtures.user(id = "a-1", role = Role.ADMIN))

        assertEquals(Role.ADMIN, tokenService.claims.single().role)
    }

    @Test
    fun `issue usa papel USER por padrao`() {
        issuer.issue(Fixtures.user())

        assertEquals(Role.USER, tokenService.claims.single().role)
    }

    @Test
    fun `issue define issuedAt com o instante do clock`() {
        issuer.issue(Fixtures.user())

        assertEquals(Fixtures.NOW, tokenService.claims.single().issuedAt)
    }

    @Test
    fun `issue calcula expiresAt somando a expiracao`() {
        issuer.issue(Fixtures.user())

        assertEquals(Fixtures.NOW.plus(expiration), tokenService.claims.single().expiresAt)
    }

    @Test
    fun `expiracao customizada e respeitada`() {
        val shortIssuer = TokenIssuer(tokenService, TestClock.fixed, Duration.ofMinutes(15))

        shortIssuer.issue(Fixtures.user())

        val claims = tokenService.claims.last()
        assertEquals(Duration.ofMinutes(15), Duration.between(claims.issuedAt, claims.expiresAt))
    }

    @Test
    fun `emite um token por chamada`() {
        issuer.issue(Fixtures.user(id = "u-1"))
        issuer.issue(Fixtures.user(id = "u-1"))

        assertEquals(2, tokenService.claims.size)
    }

    @Test
    fun `tokens de usuarios diferentes sao diferentes`() {
        val first = issuer.issue(Fixtures.user(id = "u-1"))
        val second = issuer.issue(Fixtures.user(id = "u-2"))

        assertNotEquals(first, second)
    }
}
