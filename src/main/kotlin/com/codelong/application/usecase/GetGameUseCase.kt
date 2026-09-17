package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId

interface GetGameUseCase {
    fun get(gameId: GameId, actorId: UserId): Game
}


class GetGameUseCaseImpl(
    private val gameRepository: GameRepository
) : GetGameUseCase {


    override fun get(gameId: GameId, actorId: UserId): Game {
        val game = gameRepository.findById(gameId)
            ?: throw Errors.gameNotFound()
        game.requireOwner(actorId)
        return game
    }
}