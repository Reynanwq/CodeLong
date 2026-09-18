package com.codelong.application.service

import com.codelong.domain.model.Game
import com.codelong.domain.model.Question
import com.codelong.domain.model.User
import com.codelong.domain.service.GameSequencer
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.GameSetup
import com.codelong.domain.valueobject.Ids
import java.time.Clock

/**
 * Servico de aplicacao que monta uma partida: sequencia as perguntas ativas
 * conforme o [GameMode] e cria o agregado [Game] com o snapshot persistido.
 */
interface GameFactory {
    fun start(user: User, activeQuestions: List<Question>, mode: GameMode): Game
}

class DefaultGameFactory(
    private val sequencer: GameSequencer,
    private val clock: Clock
) : GameFactory {

    override fun start(user: User, activeQuestions: List<Question>, mode: GameMode): Game {
        val sequence = sequencer.sequence(activeQuestions, mode)
        val setup = GameSetup(
            userId = user.id,
            username = user.usernameText,
            questions = sequence,
            mode = mode
        )
        return Game.newGame(Ids.newGameId(), setup, clock.instant())
    }
}
