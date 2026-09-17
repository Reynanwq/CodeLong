package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.ChangeUserStatusCommand
import com.codelong.domain.model.User
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId
import java.time.Clock

interface ChangeUserStatusUseCase {
    fun change(command: ChangeUserStatusCommand, actorId: UserId): User
}


/**
 * Ativa ou desativa contas de usuario. Operacao restrita a administradores;
 * um administrador nao pode desativar a propria conta, evitando que ele perca
 * o acesso a area administrativa por engano.
 */
class ChangeUserStatusUseCaseImpl(
    private val userRepository: UserRepository,
    private val clock: Clock
) : ChangeUserStatusUseCase {


    override fun change(command: ChangeUserStatusCommand, actorId: UserId): User {
        (!command.active && command.userId == actorId).takeIf { it }?.let {
            throw Errors.selfDeactivation()
        }

        val user = userRepository.findById(command.userId)
            ?: throw Errors.userNotFound()

        val now = clock.instant()
        user.changeStatus(command.active, now)

        return userRepository.save(user)
    }
}
