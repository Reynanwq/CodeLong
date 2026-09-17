package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.ChangePasswordCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.domain.model.User
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId
import java.time.Clock

interface ChangePasswordUseCase {
    fun change(command: ChangePasswordCommand, actorId: UserId): User
}


/**
 * Troca a senha do proprio usuario autenticado.
 *
 * Exige a senha atual como confirmacao, aplica a politica de senha e recusa a
 * reutilizacao da senha vigente. Como o JWT e stateless, tokens ja emitidos
 * continuam validos ate expirarem.
 */
class ChangePasswordUseCaseImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicy: PasswordPolicy,
    private val clock: Clock
) : ChangePasswordUseCase {


    override fun change(command: ChangePasswordCommand, actorId: UserId): User {
        val user = userRepository.findById(actorId)
            ?: throw Errors.userNotFound()

        passwordEncoder.matches(command.currentPassword, user.passwordHash()).takeUnless { it }?.let {
            throw Errors.invalidCurrentPassword()
        }

        passwordPolicy.requireStrong(command.newPassword)

        passwordEncoder.matches(command.newPassword, user.passwordHash()).takeIf { it }?.let {
            throw Errors.passwordUnchanged()
        }

        user.changePassword(passwordEncoder.encode(command.newPassword), clock.instant())
        return userRepository.save(user)
    }
}
