package com.codelong.application.usecase

import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.UserId

interface GetInProgressGameUseCase {
    fun current(userId: UserId): Game?
}


/**
 * Retorna a partida em andamento do usuario (se existir), permitindo retomar
 * o jogo mesmo perdendo o identificador da partida.
 */
class GetInProgressGameUseCaseImpl(
    private val gameRepository: GameRepository
) : GetInProgressGameUseCase {


    override fun current(userId: UserId): Game? =
        gameRepository.findInProgressByUserId(userId)
}
