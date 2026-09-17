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
    fun idText(): String = id.value

    fun usernameText(): String = profile.usernameText()

    fun emailText(): String = profile.emailText()

    fun passwordHashText(): String = profile.passwordHashText()

    fun roleName(): String = profile.roleName()

    fun statusName(): String = status.name
}
