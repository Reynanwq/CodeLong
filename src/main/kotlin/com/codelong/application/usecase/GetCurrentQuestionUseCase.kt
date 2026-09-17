package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.QuestionPublic
import com.codelong.domain.valueobject.UserId

class GetCurrentQuestionUseCase(
    private val gameRepository: GameRepository
) {

    fun current(gameId: GameId, actorId: UserId): QuestionPublic {
        val game = gameRepository.findById(gameId)
            ?: throw DomainException.notFound("GAME_NOT_FOUND", "Game not found")
        game.requireOwner(actorId)
        return game.currentQuestion().publicView()
    }
}