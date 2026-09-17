package com.codelong.application.usecase

import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.UserId

/**
 * Retorna a partida em andamento do usuario (se existir), permitindo retomar
 * o jogo mesmo perdendo o identificador da partida.
 */
class GetInProgressGameUseCase(
    private val gameRepository: GameRepository
) {

    fun current(userId: UserId): Game? =
        gameRepository.findInProgressByUserId(userId)
}
