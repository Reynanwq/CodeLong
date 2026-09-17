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

    fun usernameText(): String = username.value

    fun emailText(): String = email.value

    fun passwordHashText(): String = passwordHash.value

    fun roleName(): String = role.name
}
