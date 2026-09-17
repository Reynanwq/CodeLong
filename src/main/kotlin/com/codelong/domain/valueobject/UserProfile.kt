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

    val usernameText: String get() = username.value

    val emailText: String get() = email.value

    val passwordHashText: String get() = passwordHash.value

    val roleName: String get() = role.name
}
