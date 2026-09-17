package com.codelong.application.result

import com.codelong.domain.model.Game

/**
 * Resultado da abertura de uma partida: [created] indica se uma nova partida
 * foi criada ou se uma partida em andamento foi retomada.
 */
data class GameCreationResult(
    val game: Game,
    val created: Boolean
)
