package com.codelong.application.usecase

import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId
import java.time.Clock

class AbandonGameUseCase(
    private val gameRepository: GameRepository,
    private val clock: Clock
) {

    fun abandon(gameId: GameId, actorId: UserId): Game {
        val game = gameRepository.findById(gameId)
            ?: throw NotFoundException("GAME_NOT_FOUND", "Game not found")
        game.requireOwner(actorId)

        game.abandon(clock.instant())
        return gameRepository.save(game)
    }
}