package com.codelong.infrastructure.security

import com.codelong.domain.port.TokenService
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.TokenClaims
import com.codelong.domain.valueobject.UserId
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant

class JwtAuthenticationFilterTest {

    private class StubTokenService(
        private val result: (String) -> TokenClaims
    ) : TokenService {
        val parsed = mutableListOf<String>()

        override fun generate(claims: TokenClaims): String = "generated"

        override fun parse(token: String): TokenClaims {
            parsed.add(token)
            return result(token)
        }
    }

    private val claims = TokenClaims(
        userId = UserId("u-1"),
        role = Role.USER,
        issuedAt = Instant.parse("2030-01-01T12:00:00Z"),
        expiresAt = Instant.parse("2030-01-01T14:00:00Z")
    )

    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private var chainCalled = false

    private val chain = FilterChain { _, _ -> chainCalled = true }

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        chainCalled = false
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun runFilter(tokenService: TokenService) {
        JwtAuthenticationFilter(tokenService).doFilter(request, response, chain)
    }

    private fun authentication() = SecurityContextHolder.getContext().authentication

    @Test
    fun `sem header nao autentica e segue a cadeia`() {
        runFilter(StubTokenService { claims })

        assertNull(authentication())
        assertTrue(chainCalled)
    }

    @Test
    fun `header sem prefixo Bearer nao autentica`() {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")

        runFilter(StubTokenService { claims })

        assertNull(authentication())
        assertTrue(chainCalled)
    }

    @Test
    fun `header apenas com a palavra Bearer nao autentica`() {
        request.addHeader("Authorization", "Bearer")

        runFilter(StubTokenService { claims })

        assertNull(authentication())
    }

    @Test
    fun `prefixo em minusculo nao autentica`() {
        request.addHeader("Authorization", "bearer token")

        runFilter(StubTokenService { claims })

        assertNull(authentication())
    }

    @Test
    fun `token valido publica a autenticacao`() {
        request.addHeader("Authorization", "Bearer token-valido")
        val tokenService = StubTokenService { claims }

        runFilter(tokenService)

        val auth = authentication()
        assertNotNull(auth)
        assertEquals(AuthenticatedUser(UserId("u-1"), Role.USER), auth!!.principal)
        assertEquals("ROLE_USER", auth.authorities.first().authority)
        assertEquals(listOf("token-valido"), tokenService.parsed)
    }

    @Test
    fun `token valido de admin publica a autoridade de admin`() {
        request.addHeader("Authorization", "Bearer token-admin")

        runFilter(StubTokenService { claims.copy(role = Role.ADMIN) })

        val auth = authentication()
        assertEquals(AuthenticatedUser(UserId("u-1"), Role.ADMIN), auth!!.principal)
        assertEquals("ROLE_ADMIN", auth.authorities.first().authority)
    }

    @Test
    fun `remove espacos do token`() {
        request.addHeader("Authorization", "Bearer    token-com-espacos   ")
        val tokenService = StubTokenService { claims }

        runFilter(tokenService)

        assertEquals(listOf("token-com-espacos"), tokenService.parsed)
    }

    @Test
    fun `token invalido limpa o contexto e segue a cadeia`() {
        request.addHeader("Authorization", "Bearer invalido")

        runFilter(StubTokenService { throw JwtException("invalido") })

        assertNull(authentication())
        assertTrue(chainCalled)
    }

    @Test
    fun `token vazio limpa o contexto`() {
        request.addHeader("Authorization", "Bearer ")

        runFilter(StubTokenService { throw IllegalArgumentException("vazio") })

        assertNull(authentication())
        assertTrue(chainCalled)
    }

    @Test
    fun `erro inesperado do parser propaga`() {
        request.addHeader("Authorization", "Bearer token")

        val error = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            runFilter(StubTokenService { throw IllegalStateException("falha inesperada") })
        }

        assertEquals("falha inesperada", error.message)
    }

    @Test
    fun `cadeia e chamada mesmo com token valido`() {
        request.addHeader("Authorization", "Bearer token")

        runFilter(StubTokenService { claims })

        assertTrue(chainCalled)
    }

    @Test
    fun `nao altera o status da resposta`() {
        request.addHeader("Authorization", "Bearer token")

        runFilter(StubTokenService { claims })

        assertEquals(200, response.status)
    }

    @Test
    fun `autenticacao anterior e substituida`() {
        SecurityContextHolder.getContext().authentication =
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken("antigo", null)
        request.addHeader("Authorization", "Bearer token")

        runFilter(StubTokenService { claims })

        assertEquals(AuthenticatedUser(UserId("u-1"), Role.USER), authentication()!!.principal)
    }

    @Test
    fun `token invalido nao mantem autenticacao anterior`() {
        SecurityContextHolder.getContext().authentication =
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken("antigo", null)
        request.addHeader("Authorization", "Bearer token")

        runFilter(StubTokenService { throw JwtException("invalido") })

        assertNull(authentication())
    }
}
