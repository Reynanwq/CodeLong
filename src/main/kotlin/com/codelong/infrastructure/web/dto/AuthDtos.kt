package com.codelong.infrastructure.web.dto

import com.codelong.application.result.AuthenticationResult
import com.codelong.domain.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterUserRequest(
    @field:NotBlank(message = "username is required")
    val username: String,

    @field:NotBlank(message = "email is required")
    @field:Email(message = "email is invalid")
    val email: String,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, max = 72, message = "password must have between 8 and 72 characters")
    val password: String
)

data class LoginRequest(
    @field:NotBlank(message = "identifier is required")
    val identifier: String,

    @field:NotBlank(message = "password is required")
    val password: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id.value,
            username = user.username.value,
            email = user.email.value,
            role = user.role.name,
            createdAt = user.createdAt
        )
    }
}

data class AuthResponse(
    val token: String,
    val tokenType: String,
    val user: UserResponse
) {
    companion object {
        fun from(result: AuthenticationResult) = AuthResponse(
            token = result.token,
            tokenType = "Bearer",
            user = UserResponse.from(result.user)
        )
    }
}