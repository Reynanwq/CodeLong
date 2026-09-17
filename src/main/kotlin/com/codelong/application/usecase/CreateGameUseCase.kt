package com.codelong.application.usecase

import com.codelong.application.result.GameCreationResult
import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.DomainException
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId

/**
 * Abre uma partida para o usuario.
 *
 * Se ja existe uma partida em andamento, ela e retomada em vez de criar uma
 * segunda — o jogador nunca fica com duas partidas abertas ao mesmo tempo.
 */
class CreateGameUseCase(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val gameRepository: GameRepository,
    private val gameFactory: GameFactory
) {

    fun create(actorId: UserId): GameCreationResult {
        val user = userRepository.findById(actorId)
            ?: throw DomainException.notFound("USER_NOT_FOUND", "User not found")

        user.isActive().takeUnless { it }?.let {
            throw DomainException.forbidden("ACCOUNT_INACTIVE", "This account is not active")
        }

        gameRepository.findInProgressByUserId(actorId)?.let {
            return GameCreationResult(game = it, created = false)
        }

        val activeQuestions = questionRepository.findAllActive()
        activeQuestions.isEmpty().takeIf { it }?.let {
            throw DomainException.conflict(
                "NO_ACTIVE_QUESTIONS",
                "There are no active questions available to start a game"
            )
        }

        val game = gameRepository.save(gameFactory.start(user, activeQuestions))
        return GameCreationResult(game = game, created = true)
    }
}
