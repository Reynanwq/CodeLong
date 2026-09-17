package com.codelong.domain.valueobject

/**
 * Dados de identidade e credenciais de um usuario.
 */
data class UserProfile(
    val username: Username,
    val email: Email,
    val passwordHash: PasswordHash,
    val role: Role
) {
    fun withPasswordHash(newHash: PasswordHash): UserProfile = copy(passwordHash = newHash)
}