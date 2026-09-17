package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangePasswordCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.domain.model.User
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId
import java.time.Clock

/**
 * Troca a senha do proprio usuario autenticado.
 *
 * Exige a senha atual como confirmacao, aplica a politica de senha e recusa a
 * reutilizacao da senha vigente. Como o JWT e stateless, tokens ja emitidos
 * continuam validos ate expirarem.
 */
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicy: PasswordPolicy,
    private val clock: Clock
) {

    fun change(command: ChangePasswordCommand, actorId: UserId): User {
        val user = userRepository.findById(actorId)
            ?: throw DomainException.notFound("USER_NOT_FOUND", "User not found")

        if (!passwordEncoder.matches(command.currentPassword, user.passwordHash())) {
            throw DomainException.unauthorized(
                "INVALID_CURRENT_PASSWORD",
                "The current password is incorrect"
            )
        }

        passwordPolicy.requireStrong(command.newPassword)

        if (passwordEncoder.matches(command.newPassword, user.passwordHash())) {
            throw DomainException.invalidInput(
                "password.unchanged",
                "The new password must differ from the current one"
            )
        }

        user.changePassword(passwordEncoder.encode(command.newPassword), clock.instant())
        return userRepository.save(user)
    }
}
