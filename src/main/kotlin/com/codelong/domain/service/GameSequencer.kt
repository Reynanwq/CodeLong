package com.codelong.domain.service

import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.GameQuestion
import kotlin.random.Random

/**
 * Monta a sequencia de perguntas de uma partida.
 *
 * - [GameMode.CLASSIC]: dificuldade sempre crescente (nivel 1 .. 10). Dentro de
 *   um mesmo nivel a ordem e aleatoria. Nunca ha shuffle global.
 * - [GameMode.GENOCIDA]: ordem totalmente aleatoria, ignorando a dificuldade.
 *
 * Em ambos os modos as alternativas de cada pergunta sao embaralhadas, para que
 * a posicao da resposta correta varie entre partidas.
 *
 * O [random] e injetado para permitir comportamento deterministico em testes.
 */
class GameSequencer(private val random: Random) {

    fun sequence(activeQuestions: List<Question>, mode: GameMode = GameMode.CLASSIC): List<GameQuestion> {
        val active = activeQuestions.filter { it.isActive }

        return ordered(active, mode)
            .map { it.snapshot().withShuffledOptions(random) }
    }

    private fun ordered(active: List<Question>, mode: GameMode): List<Question> = when (mode) {
        GameMode.GENOCIDA -> active.shuffled(random)
        GameMode.CLASSIC -> active
            .groupBy { it.difficulty }
            .toSortedMap()
            .values
            .flatMap { it.shuffled(random) }
    }
}
