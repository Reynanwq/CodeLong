package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors



/**
 * Os 10 niveis de dificuldade do CodeLong.
 *
 * Cada nivel possui um numero inteiro (1..10) usado para ordenacao natural
 * crescente durante a sequencia da partida e a pontuacao base concedida por
 * acerto naquele nivel.
 */
enum class Difficulty(val level: Int, val points: Int) : Comparable<Difficulty> {
    VERY_EASY(1, 10),
    EASY(2, 20),
    EASY_PLUS(3, 30),
    MEDIUM(4, 40),
    MEDIUM_PLUS(5, 50),
    HARD(6, 60),
    HARD_PLUS(7, 70),
    VERY_HARD(8, 80),
    EXPERT(9, 90),
    MASTER(10, 100);

    companion object {
        private const val MIN_LEVEL = 1
        private const val MAX_LEVEL = 10

        fun fromLevel(level: Int): Difficulty =
            entries.firstOrNull { it.level == level }
                ?: throw Errors.unknownDifficultyLevel(MIN_LEVEL, MAX_LEVEL)

        val orderedByLevel: List<Difficulty> = entries.sortedBy { it.level }
    }
}