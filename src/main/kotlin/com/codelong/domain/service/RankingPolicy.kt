package com.codelong.domain.service

import com.codelong.domain.valueobject.RankEntry

/**
 * Politica deterministica de ordenacao e desempate do ranking.
 *
 * Ordem:
 * 1. maior pontuacao;
 * 2. maior numero de acertos;
 * 3. menor tempo total (duracao da partida);
 * 4. data de obtencao da pontuacao (mais antiga primeiro).
 *
 * O ranking considera apenas a melhor partida de cada usuario.
 */
object RankingPolicy {

    val comparator: Comparator<RankEntry> =
        compareByDescending<RankEntry> { it.score }
            .thenByDescending { it.correctAnswers }
            .thenBy { it.totalTimeMillis }
            .thenBy { it.achievedAt }

    fun isBetter(candidate: RankEntry, than: RankEntry): Boolean =
        comparator.compare(candidate, than) < 0
}