package com.codelong.application.usecase

import com.codelong.application.command.GameSearchQuery
import com.codelong.domain.port.GamePage
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.GameSearch
import com.codelong.domain.valueobject.UserId

/**
 * Historico de partidas do proprio usuario, paginado e opcionalmente filtrado
 * por status. O [UserId] vem sempre do token, nunca do cliente.
 */
class ListGamesUseCase(
    private val gameRepository: GameRepository
) {

    fun list(userId: UserId, query: GameSearchQuery): GamePage =
        gameRepository.search(
            GameSearch(
                userId = userId,
                status = query.status,
                page = query.page,
                size = query.size
            )
        )
}
