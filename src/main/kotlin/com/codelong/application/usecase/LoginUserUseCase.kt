package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.LoginCommand
import com.codelong.application.result.AuthenticationResult
import com.codelong.application.service.TokenIssuer
import com.codelong.domain.model.User
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username

interface LoginUserUseCase {
    fun login(command: LoginCommand): AuthenticationResult
}


class LoginUserUseCaseImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenIssuer: TokenIssuer
) : LoginUserUseCase {


    override fun login(command: LoginCommand): AuthenticationResult {
        val user = resolveUser(command.identifier)
            ?: throw Errors.invalidCredentials()

        passwordEncoder.matches(command.password, user.passwordHash()).takeUnless { it }?.let {
            throw Errors.invalidCredentials()
        }
        user.isActive().takeUnless { it }?.let {
            throw Errors.accountInactiveUnauthorized()
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