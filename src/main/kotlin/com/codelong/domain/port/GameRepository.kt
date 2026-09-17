package com.codelong.domain.port

import com.codelong.domain.model.Game
import com.codelong.domain.valueobject.GameId

interface GameRepository {
    fun save(game: Game): Game
    fun findById(id: GameId): Game?
}