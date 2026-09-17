package com.codelong.application.service

import com.codelong.domain.model.Game
import com.codelong.domain.model.Question
import com.codelong.domain.model.User
import com.codelong.domain.service.GameSequencer
import com.codelong.domain.valueobject.GameSetup
import com.codelong.domain.valueobject.Ids
import java.time.Clock

/**
 * Servico de aplicacao que monta uma partida: sequencia as perguntas ativas e
 * cria o agregado [Game] com o snapshot persistido.
 */
class GameFactory(
    private val sequencer: GameSequencer,
    private val clock: Clock
) {

    fun start(user: User, activeQuestions: List<Question>): Game {
        val sequence = sequencer.sequence(activeQuestions)
        val setup = GameSetup(
            userId = user.id,
            username = user.username.value,
            questions = sequence
        )
        return Game.newGame(Ids.newGameId(), setup, clock.instant())
    }
}