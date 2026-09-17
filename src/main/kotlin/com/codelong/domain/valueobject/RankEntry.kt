package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Entrada do ranking: a melhor partida (maior pontuacao) de cada usuario.
 */
data class RankEntry(
    val userId: UserId,
    val username: String,
    val score: Int,
    val correctAnswers: Int,
    val totalTimeMillis: Long,
    val achievedAt: Instant,
    val position: Int? = null
) {
    val userIdText: String get() = userId.value
}