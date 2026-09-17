package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.LoginCommand
import com.codelong.application.result.AuthenticationResult
import com.codelong.application.service.TokenIssuer
import com.codelong.domain.model.User
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username

class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenIssuer: TokenIssuer
) {

    fun login(command: LoginCommand): AuthenticationResult {
        val user = resolveUser(command.identifier)
            ?: throw DomainException.unauthorized("INVALID_CREDENTIALS", "Invalid credentials")

        passwordEncoder.matches(command.password, user.passwordHash()).takeUnless { it }?.let {
            throw DomainException.unauthorized("INVALID_CREDENTIALS", "Invalid credentials")
        }
        user.isActive().takeUnless { it }?.let {
            throw DomainException.unauthorized("ACCOUNT_INACTIVE", "This account is not active")
        }

        return AuthenticationResult(user = user, token = tokenIssuer.issue(user))
    }

    private fun resolveUser(identifier: String): User? =
        when {
            identifier.contains('@') -> Email.isValid(identifier)
                .takeIf { it }
                ?.let { userRepository.findByEmail(Email.of(identifier)) }

            Username.isValid(identifier) -> userRepository.findByUsername(Username.of(identifier))
            else -> null
        }
}