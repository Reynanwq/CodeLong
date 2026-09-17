package com.codelong.domain.model

import com.codelong.domain.exception.ConflictException
import com.codelong.domain.exception.ForbiddenException
import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameTest {

    @Test
    fun `resposta correta soma os pontos da dificuldade e avanca`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))

        val result = game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertEquals(Difficulty.EASY.points, result.record.earnedPoints)
        assertEquals(Difficulty.EASY.points, game.score())
        assertEquals(1, game.correctAnswersCount())
        assertEquals(0, game.wrongAnswersCount())
        assertEquals(1, game.currentQuestionIndex())
        assertTrue(game.isInProgress())
    }

    @Test
    fun `resposta incorreta nao pontua mas avanca`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.HARD, Difficulty.HARD))
        val question = game.currentQuestion()
        val wrongOption = question.options.first { it.id != question.correctOption }.id

        val result = game.answer(wrongOption, Fixtures.NOW)

        assertEquals(0, result.record.earnedPoints)
        assertFalse(result.record.correct)
        assertEquals(0, game.score())
        assertEquals(1, game.wrongAnswersCount())
        assertEquals(1, game.currentQuestionIndex())
    }

    @Test
    fun `responder a ultima pergunta conclui a partida`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.MASTER))

        val result = game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertTrue(result.gameCompleted)
        assertTrue(game.isCompleted())
        assertEquals(GameStatus.COMPLETED, game.status())
        assertEquals(Fixtures.NOW, game.completedAt())
        assertEquals(0, game.remainingQuestions())
        assertEquals(1, game.currentQuestionIndex())
    }

    @Test
    fun `nao aceita resposta apos conclusao`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertThrows<ConflictException> {
            game.answer(OptionId("opt-3"), Fixtures.NOW)
        }
    }

    @Test
    fun `opcao inexistente e rejeitada`() {
        val game = Fixtures.game()

        assertThrows<InvalidInputException> {
            game.answer(OptionId("nao-existe"), Fixtures.NOW)
        }
    }

    @Test
    fun `abandonar encerra a partida e bloqueia novas respostas`() {
        val game = Fixtures.game()

        game.abandon(Fixtures.NOW)

        assertEquals(GameStatus.ABANDONED, game.status())
        assertFalse(game.isInProgress())
        assertThrows<ConflictException> {
            game.answer(game.questions().first().correctOption, Fixtures.NOW)
        }
    }

    @Test
    fun `somente o dono acessa a partida`() {
        val game = Fixtures.game(userId = "u-1")

        assertThrows<ForbiddenException> {
            game.requireOwner(UserId("u-2"))
        }
        game.requireOwner(UserId("u-1"))
    }

    @Test
    fun `reconstituicao preserva o estado`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.HARD, Difficulty.MASTER))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        val restored = Game.reconstitute(game.state())

        assertEquals(game.score(), restored.score())
        assertEquals(game.currentQuestionIndex(), restored.currentQuestionIndex())
        assertEquals(game.status(), restored.status())
        assertEquals(game.answers().size, restored.answers().size)
        assertEquals(game.questions().size, restored.questions().size)
        assertNotNull(restored.currentQuestion())
    }
}