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
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length).trim())
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(token: String) {
        try {
            val claims = tokenService.parse(token)
            val principal = AuthenticatedUser(claims.userId, claims.role)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims.role.name}"))
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        } catch (ex: JwtException) {
            SecurityContextHolder.clearContext()
        } catch (ex: IllegalArgumentException) {
            SecurityContextHolder.clearContext()
        }
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}