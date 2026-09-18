package com.codelong.application.usecase

import com.codelong.application.result.GameCreationResult
import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.Errors
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.UserId

/**
 * Abre uma partida para o usuario no modo informado.
 *
 * Se ja existe uma partida em andamento, ela e retomada em vez de criar uma
 * segunda — o jogador nunca fica com duas partidas abertas ao mesmo tempo.
 */
interface CreateGameUseCase {
    fun create(actorId: UserId, mode: GameMode): GameCreationResult
}

class CreateGameUseCaseImpl(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val gameRepository: GameRepository,
    private val gameFactory: GameFactory
) : CreateGameUseCase {

    override fun create(actorId: UserId, mode: GameMode): GameCreationResult {
        val user = userRepository.findById(actorId)
            ?: throw Errors.userNotFound()

        user.isActive.takeUnless { it }?.let {
            throw Errors.accountInactiveForbidden()
        }

        gameRepository.findInProgressByUserId(actorId)?.let {
            return GameCreationResult(game = it, created = false)
        }

        val activeQuestions = questionRepository.findAllActive()
        activeQuestions.isEmpty().takeIf { it }?.let {
            throw Errors.noActiveQuestions()
        }

        val game = gameRepository.save(gameFactory.start(user, activeQuestions, mode))
        return GameCreationResult(game = game, created = true)
    }
}
