package com.codelong.domain.service

import com.codelong.domain.valueobject.RankEntry

/**
 * Politica deterministica de ordenacao e desempate do ranking.
 *
 * Ordem:
 * 1. maior pontuacao (premia quem foi mais longe e acertou as mais dificeis);
 * 2. menor tempo total (duracao da partida);
 * 3. maior numero de acertos;
 * 4. data de obtencao da pontuacao (mais antiga primeiro).
 *
 * O ranking considera apenas a melhor partida de cada usuario, concluida ou
 * abandonada, desde que tenha respondido o minimo de perguntas exigido.
 */
object RankingPolicy {

    val comparator: Comparator<RankEntry> =
        compareByDescending<RankEntry> { it.score }
            .thenBy { it.totalTimeMillis }
            .thenByDescending { it.correctAnswers }
            .thenBy { it.achievedAt }

    fun isBetter(candidate: RankEntry, than: RankEntry): Boolean =
        comparator.compare(candidate, than) < 0
}