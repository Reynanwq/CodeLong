package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId
import java.time.Clock

interface AbandonGameUseCase {
    fun abandon(gameId: GameId, actorId: UserId): Game
}


class AbandonGameUseCaseImpl(
    private val gameRepository: GameRepository,
    private val clock: Clock
) : AbandonGameUseCase {


    override fun abandon(gameId: GameId, actorId: UserId): Game {
        val game = gameRepository.findById(gameId)
            ?: throw Errors.gameNotFound()
        game.requireOwner(actorId)

        game.abandon(clock.instant())
        return gameRepository.save(game)
    }
}