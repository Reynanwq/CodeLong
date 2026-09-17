package com.codelong.domain.valueobject

/**
 * Dados necessarios para iniciar uma partida.
 */
data class GameSetup(
    val userId: UserId,
    val username: String,
    val questions: List<GameQuestion>
)