package com.codelong.domain.port

import com.codelong.domain.model.Game
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId

data class GameSearch(
    val userId: UserId,
    val status: GameStatus? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    init {
        Pagination.requireValid(page, size)
    }
}

data class GamePage(
    val items: List<Game>,
    val totalElements: Long,
    val page: Int,
    val size: Int
)

interface GameRepository {
    fun save(game: Game): Game
    fun findById(id: GameId): Game?
    fun findInProgressByUserId(userId: UserId): Game?
    fun search(search: GameSearch): GamePage
}
