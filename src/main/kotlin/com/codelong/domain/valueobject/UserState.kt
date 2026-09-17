package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Estado completo de um [com.codelong.domain.model.User] reconstruido pela
 * persistencia ou por testes.
 */
data class UserState(
    val id: UserId,
    val profile: UserProfile,
    val status: AccountStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)