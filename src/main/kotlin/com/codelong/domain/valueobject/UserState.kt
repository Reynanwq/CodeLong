package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Estado completo de um [com.codelong.domain.model.User] reconstruido pela
 * persistencia ou por testes. Expoe o proprio conteudo sem obrigar o chamador a
 * atravessar os objetos internos.
 */
data class UserState(
    val id: UserId,
    val profile: UserProfile,
    val status: AccountStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val idText: String get() = id.value

    val usernameText: String get() = profile.usernameText

    val emailText: String get() = profile.emailText

    val passwordHashText: String get() = profile.passwordHashText

    val roleName: String get() = profile.roleName

    val statusName: String get() = status.name
}
