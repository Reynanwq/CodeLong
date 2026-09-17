package com.codelong.domain.service

import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.GameQuestion
import kotlin.random.Random

/**
 * Monta a sequencia de perguntas de uma partida.
 *
 * Regra: dificuldade sempre crescente (nivel 1 .. 10). Dentro de um mesmo
 * nivel a ordem e aleatoria. Nunca ha shuffle global.
 *
 * O [random] e injetado para permitir comportamento deterministico em testes.
 */
class GameSequencer(private val random: Random) {

    fun sequence(activeQuestions: List<Question>): List<GameQuestion> {
        return activeQuestions
            .asSequence()
            .filter { it.isActive() }
            .groupBy { it.difficulty() }
            .toSortedMap()
            .values
            .flatMap { questionsOfSameLevel ->
                questionsOfSameLevel
                    .shuffled(random)
                    .map { it.snapshot() }
            }
            .toList()
    }
}