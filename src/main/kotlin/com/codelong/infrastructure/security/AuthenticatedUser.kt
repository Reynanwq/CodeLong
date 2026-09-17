package com.codelong.infrastructure.security

import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId

/**
 * Principal autenticado a partir do JWT, disponibilizado no SecurityContext.
 */
data class AuthenticatedUser(
    val userId: UserId,
    val role: Role
)