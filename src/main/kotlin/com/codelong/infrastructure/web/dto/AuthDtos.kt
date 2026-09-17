package com.codelong.infrastructure.web.dto

import com.codelong.application.result.AuthenticationResult
import com.codelong.domain.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

private const val USERNAME_REQUIRED = "username is required"
private const val EMAIL_REQUIRED = "email is required"
private const val EMAIL_INVALID = "email is invalid"
private const val PASSWORD_REQUIRED = "password is required"
private const val PASSWORD_SIZE = "password must have between 8 and 72 characters"
private const val IDENTIFIER_REQUIRED = "identifier is required"
private const val CURRENT_PASSWORD_REQUIRED = "currentPassword is required"
private const val NEW_PASSWORD_REQUIRED = "newPassword is required"
private const val BEARER_TOKEN_TYPE = "Bearer"

data class RegisterUserRequest(
    @field:NotBlank(message = USERNAME_REQUIRED)
    val username: String,

    @field:NotBlank(message = EMAIL_REQUIRED)
    @field:Email(message = EMAIL_INVALID)
    val email: String,

    @field:NotBlank(message = PASSWORD_REQUIRED)
    @field:Size(min = 8, max = 72, message = PASSWORD_SIZE)
    val password: String
)

data class LoginRequest(
    @field:NotBlank(message = IDENTIFIER_REQUIRED)
    val identifier: String,

    @field:NotBlank(message = PASSWORD_REQUIRED)
    val password: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = CURRENT_PASSWORD_REQUIRED)
    val currentPassword: String,

    @field:NotBlank(message = NEW_PASSWORD_REQUIRED)
    @field:Size(min = 8, max = 72, message = PASSWORD_SIZE)
    val newPassword: String
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
            id = user.idText(),
            username = user.usernameText(),
            email = user.emailText(),
            role = user.roleName(),
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
            tokenType = BEARER_TOKEN_TYPE,
            user = UserResponse.from(result.user)
        )
    }
}
