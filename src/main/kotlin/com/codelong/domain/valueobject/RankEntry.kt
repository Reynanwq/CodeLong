package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Entrada do ranking: uma tentativa (partida) de um usuario.
 *
 * [answeredQuestions] mostra ate onde o jogador foi, [wrongAnswers] quantos
 * erros cometeu, [mode] o modo jogado e [totalTimeMillis] a duracao, usada como
 * desempate.
 */
data class RankEntry(
    val userId: UserId,
    val username: String,
    val score: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val answeredQuestions: Int,
    val mode: GameMode,
    val totalTimeMillis: Long,
    val achievedAt: Instant,
    val position: Int? = null
) {
    val userIdText: String get() = userId.value
}
