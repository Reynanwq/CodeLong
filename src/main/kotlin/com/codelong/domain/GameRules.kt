package com.codelong.domain

import java.time.Duration

/**
 * Regras de tempo do jogo.
 */
object GameRules {

    /** Tempo maximo, em segundos, para responder a pergunta atual. */
    const val ANSWER_TIME_LIMIT_SECONDS = 20L

    /** Duracao maxima para responder a pergunta atual. */
    val ANSWER_TIME_LIMIT: Duration = Duration.ofSeconds(ANSWER_TIME_LIMIT_SECONDS)

    /**
     * Minimo de perguntas respondidas para uma partida disputar o ranking.
     * Evita que alguem responda poucas perguntas e pare com aproveitamento alto.
     */
    const val MIN_ANSWERED_QUESTIONS_FOR_RANKING = 10
}
