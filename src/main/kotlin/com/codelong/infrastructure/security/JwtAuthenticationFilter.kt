package com.codelong.infrastructure.security

import com.codelong.domain.port.TokenService
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Extrai o JWT do header Authorization, valida e publica a autenticacao no
 * SecurityContext. Tokens invalidos nao interrompem a cadeia: a requisicao
 * segue sem autenticacao e e barrada pelo entry point quando exigido.
 */
class JwtAuthenticationFilter(
    private val tokenService: TokenService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.let(::bearerToken)
            ?.let(::authenticate)

        filterChain.doFilter(request, response)
    }

    /** Extrai o token do header, devolvendo `null` quando ele nao tem conteudo util. */
    private fun bearerToken(header: String): String? =
        header.removePrefix(BEARER_PREFIX).trim().takeIf { it.isNotEmpty() }

    private fun authenticate(token: String) {
        try {
            val claims = tokenService.parse(token)
            val principal = AuthenticatedUser(claims.userId, claims.role)
            val authorities = listOf(SimpleGrantedAuthority(ROLE_PREFIX + claims.role.name))
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(principal, null, authorities)
        } catch (_: JwtException) {
            SecurityContextHolder.clearContext()
        }
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
        const val ROLE_PREFIX = "ROLE_"
    }
}