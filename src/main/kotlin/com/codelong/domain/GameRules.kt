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
}
