package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.result.AuthenticationResult
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username

interface RegisterUserUseCase {
    fun register(command: RegisterUserCommand): AuthenticationResult
}


class RegisterUserUseCaseImpl(
    private val userRepository: UserRepository,
    private val userFactory: UserFactory,
    private val tokenIssuer: TokenIssuer,
    private val passwordPolicy: PasswordPolicy
) : RegisterUserUseCase {


    override fun register(command: RegisterUserCommand): AuthenticationResult {
        passwordPolicy.requireStrong(command.password)

        val username = Username.of(command.username)
        val email = Email.of(command.email)

        userRepository.existsByUsername(username).takeIf { it }?.let {
            throw Errors.usernameAlreadyExists()
        }
        userRepository.existsByEmail(email).takeIf { it }?.let {
            throw Errors.emailAlreadyExists()
        }

        val user = userRepository.save(userFactory.createUser(username, email, command.password))
        return AuthenticationResult(user = user, token = tokenIssuer.issue(user))
    }
}
