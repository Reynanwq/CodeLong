package com.codelong.application.usecase

import com.codelong.application.result.CurrentQuestionResult
import com.codelong.domain.exception.Errors
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId
import java.time.Clock

/**
 * Devolve a pergunta atual da partida.
 *
 * Se o prazo da pergunta anterior expirou, ela e contabilizada como erro e a
 * partida avanca antes da resposta, garantindo que o tempo corra no servidor.
 */
interface GetCurrentQuestionUseCase {
    fun current(gameId: GameId, actorId: UserId): CurrentQuestionResult
}

class GetCurrentQuestionUseCaseImpl(
    private val gameRepository: GameRepository,
    private val clock: Clock
) : GetCurrentQuestionUseCase {

    override fun current(gameId: GameId, actorId: UserId): CurrentQuestionResult {
        val game = gameRepository.findById(gameId)
            ?: throw Errors.gameNotFound()
        game.requireOwner(actorId)

        val now = clock.instant()
        val saved = game
            .takeIf { it.isCurrentQuestionExpired(now) }
            ?.also { it.expireCurrentQuestion(now) }
            ?.let { gameRepository.save(it) }
            ?: game

        return CurrentQuestionResult(
            question = saved.currentQuestion().publicView(),
            index = saved.currentQuestionIndex,
            total = saved.totalQuestions,
            deadline = saved.currentQuestionDeadline
        )
    }
}
