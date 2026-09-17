package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Dados embutidos no token JWT e extraidos dele para autenticacao.
 */
data class TokenClaims(
    val userId: UserId,
    val role: Role,
    val issuedAt: Instant,
    val expiresAt: Instant
) {
    val userIdText: String get() = userId.value

    val roleName: String get() = role.name
}