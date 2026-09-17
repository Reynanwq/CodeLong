package com.codelong.application.service

import com.codelong.domain.model.User
import com.codelong.domain.port.TokenService
import com.codelong.domain.valueobject.TokenClaims
import java.time.Clock
import java.time.Duration

/**
 * Servico de aplicacao responsavel por emitir tokens de acesso.
 */
interface TokenIssuer {
    fun issue(user: User): String
}

class DefaultTokenIssuer(
    private val tokenService: TokenService,
    private val clock: Clock,
    private val expiration: Duration
) : TokenIssuer {

    override fun issue(user: User): String {
        val now = clock.instant()
        return tokenService.generate(
            TokenClaims(
                userId = user.id,
                role = user.role,
                issuedAt = now,
                expiresAt = now.plus(expiration)
            )
        )
    }
}
