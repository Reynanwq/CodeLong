package com.codelong.domain

import java.time.Duration

/**
 * Regras de tempo e de elegibilidade do jogo.
 */
object GameRules {

    /** Tempo maximo, em segundos, para responder a pergunta atual. */
    const val ANSWER_TIME_LIMIT_SECONDS = 20L

    /** Duracao maxima para responder a pergunta atual. */
    val ANSWER_TIME_LIMIT: Duration = Duration.ofSeconds(ANSWER_TIME_LIMIT_SECONDS)

    /**
     * Minimo de perguntas respondidas para uma partida CLASSIC disputar o
     * ranking. Evita que alguem responda poucas perguntas e pare.
     */
    const val MIN_ANSWERED_QUESTIONS_FOR_RANKING = 10

    /**
     * Minimo de perguntas respondidas para uma partida GENOCIDA disputar o
     * ranking. E menor porque o modo e de morte subita: a resposta errada que
     * encerra a partida tambem conta, entao o jogador precisa acertar apenas
     * [MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA] - 1 vezes para se qualificar.
     */
    const val MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA = 5
}
