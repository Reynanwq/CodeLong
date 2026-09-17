package com.codelong.infrastructure.web.dto

import com.codelong.domain.model.User
import java.time.Instant

data class ChangeUserStatusRequest(
    val active: Boolean
)

data class AdminUserResponse(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(user: User) = AdminUserResponse(
            id = user.id.value,
            username = user.username.value,
            email = user.email.value,
            role = user.role.name,
            status = user.status().name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt()
        )
    }
}
