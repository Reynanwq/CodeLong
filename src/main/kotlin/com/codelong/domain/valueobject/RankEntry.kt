package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Entrada do ranking: a melhor partida (maior pontuacao) de cada usuario.
 *
 * [answeredQuestions] mostra ate onde o jogador foi e [totalTimeMillis] a
 * duracao da partida, usada como desempate.
 */
data class RankEntry(
    val userId: UserId,
    val username: String,
    val score: Int,
    val correctAnswers: Int,
    val answeredQuestions: Int,
    val totalTimeMillis: Long,
    val achievedAt: Instant,
    val position: Int? = null
) {
    val userIdText: String get() = userId.value
}
