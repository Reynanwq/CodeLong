package com.codelong.application.command

import com.codelong.domain.valueobject.GameStatus

data class GameSearchQuery(
    val status: GameStatus? = null,
    val page: Int = 0,
    val size: Int = 20
)
