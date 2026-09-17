package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Estado completo de um [com.codelong.domain.model.Game], usado para
 * reconstrucao pela persistencia ou por testes.
 */
data class GameState(
    val id: GameId,
    val userId: UserId,
    val username: String,
    val status: GameStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    val currentQuestionIndex: Int,
    val currentQuestionDeadline: Instant,
    val questions: List<GameQuestion>,
    val answers: List<AnswerRecord>,
    val score: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val version: Long
) {
    val idText: String get() = id.value

    val userIdText: String get() = userId.value

    val statusName: String get() = status.name
}
