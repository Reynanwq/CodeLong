package com.codelong.application.service

import com.codelong.application.service.DefaultGameFactory

import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.service.GameSequencer
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameFactoryTest {

    private val factory = DefaultGameFactory(GameSequencer(Random(42)), TestClock.fixed)

    @Test
    fun `start cria partida em andamento para o usuario`() {
        val user = Fixtures.user(id = "u-1", username = "alice")
        val questions = listOf(
            Fixtures.question(id = "q-1", difficulty = Difficulty.EASY),
            Fixtures.question(id = "q-2", difficulty = Difficulty.HARD)
        )

        val game = factory.start(user, questions, GameMode.CLASSIC)

        assertEquals(UserId("u-1"), game.userId)
        assertEquals("alice", game.username)
        assertEquals(GameStatus.IN_PROGRESS, game.status)
        assertEquals(Fixtures.NOW, game.startedAt)
        assertEquals(0, game.currentQuestionIndex)
        assertEquals(0, game.score)
        assertEquals(0L, game.version)
    }

    @Test
    fun `start usa o snapshot das perguntas`() {
        val user = Fixtures.user()
        val questions = listOf(Fixtures.question(id = "q-1", difficulty = Difficulty.MEDIUM))

        val game = factory.start(user, questions, GameMode.CLASSIC)

        assertEquals(1, game.totalQuestions)
        assertEquals(questions.first().id, game.questions.first().id)
        assertEquals(questions.first().statement, game.questions.first().statement)
        assertEquals(questions.first().correctOption, game.questions.first().correctOption)
    }

    @Test
    fun `start ordena as perguntas por dificuldade crescente`() {
        val user = Fixtures.user()
        val questions = listOf(
            Fixtures.question(id = "q-master", difficulty = Difficulty.MASTER),
            Fixtures.question(id = "q-easy", difficulty = Difficulty.EASY),
            Fixtures.question(id = "q-medium", difficulty = Difficulty.MEDIUM)
        )

        val game = factory.start(user, questions, GameMode.CLASSIC)

        assertEquals(
            listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MASTER),
            game.questions.map { it.difficulty }
        )
    }

    @Test
    fun `start ignora perguntas inativas`() {
        val user = Fixtures.user()
        val questions = listOf(
            Fixtures.question(id = "ativa", difficulty = Difficulty.EASY),
            Fixtures.question(id = "inativa", difficulty = Difficulty.EASY).deactivate(Fixtures.NOW)
        )

        val game = factory.start(user, questions, GameMode.CLASSIC)

        assertEquals(1, game.totalQuestions)
    }

    @Test
    fun `start sem perguntas gera partida vazia`() {
        val game = factory.start(Fixtures.user(), emptyList(), GameMode.CLASSIC)

        assertEquals(0, game.totalQuestions)
        assertEquals(0, game.remainingQuestions)
        assertTrue(game.isInProgress)
    }

    @Test
    fun `cada partida recebe um identificador distinto`() {
        val user = Fixtures.user()
        val questions = listOf(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))

        val ids = (1..100).map { factory.start(user, questions, GameMode.CLASSIC).id.value }

        assertEquals(100, ids.distinct().size)
    }

    @Test
    fun `start nao altera as perguntas de origem`() {
        val user = Fixtures.user()
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.EASY)

        factory.start(user, listOf(question), GameMode.CLASSIC)

        assertTrue(question.isActive)
        assertEquals("O que e polimorfismo?", question.statement)
    }

    @Test
    fun `start registra o username do usuario no snapshot`() {
        val game = factory.start(
            Fixtures.user(username = "carol"),
            listOf(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY)),
            GameMode.CLASSIC
        )

        assertEquals("carol", game.username)
    }
}
