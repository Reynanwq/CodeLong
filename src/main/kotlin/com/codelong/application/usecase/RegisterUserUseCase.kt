package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.result.AuthenticationResult
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username

class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val userFactory: UserFactory,
    private val tokenIssuer: TokenIssuer,
    private val passwordPolicy: PasswordPolicy
) {

    fun register(command: RegisterUserCommand): AuthenticationResult {
        passwordPolicy.requireStrong(command.password)

        val username = Username.of(command.username)
        val email = Email.of(command.email)

        userRepository.existsByUsername(username).takeIf { it }?.let {
            throw DomainException.conflict("USERNAME_ALREADY_EXISTS", "This username is already taken")
        }
        userRepository.existsByEmail(email).takeIf { it }?.let {
            throw DomainException.conflict("EMAIL_ALREADY_EXISTS", "This email is already registered")
        }

        val user = userRepository.save(userFactory.createUser(username, email, command.password))
        return AuthenticationResult(user = user, token = tokenIssuer.issue(user))
    }
}
