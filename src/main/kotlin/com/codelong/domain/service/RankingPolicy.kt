package com.codelong.domain.service

import com.codelong.domain.GameRules
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.RankEntry

/**
 * Politica deterministica de ordenacao, desempate e elegibilidade do ranking.
 *
 * Ordem:
 * 1. maior pontuacao (premia quem foi mais longe e acertou as mais dificeis);
 * 2. menor tempo total (duracao da partida);
 * 3. maior numero de acertos;
 * 4. data de obtencao da pontuacao (mais antiga primeiro).
 *
 * Cada tentativa elegivel ocupa uma linha, de qualquer modo de jogo.
 */
object RankingPolicy {

    val comparator: Comparator<RankEntry> =
        compareByDescending<RankEntry> { it.score }
            .thenBy { it.totalTimeMillis }
            .thenByDescending { it.correctAnswers }
            .thenBy { it.achievedAt }

    fun isBetter(candidate: RankEntry, than: RankEntry): Boolean =
        comparator.compare(candidate, than) < 0

    /** Minimo de respostas exigido para o modo informado. */
    fun minimumAnswers(mode: GameMode): Int = when (mode) {
        GameMode.GENOCIDA -> GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA
        GameMode.APRENDIZADO -> GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_APRENDIZADO
        GameMode.GUBEE -> GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_GUBEE
        GameMode.CLASSIC -> GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING
    }

    /** Indica se a tentativa tem respostas suficientes para entrar no ranking. */
    fun isEligible(entry: RankEntry): Boolean =
        entry.answeredQuestions >= minimumAnswers(entry.mode)
}
