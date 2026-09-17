package com.codelong.domain.model

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameAggregateTest {

    @Test
    fun `expoe identidade e dono da partida`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", username = "alice")

        assertEquals(GameId("g-1"), game.id)
        assertEquals(UserId("u-1"), game.userId)
        assertEquals("alice", game.username)
        assertEquals(Fixtures.NOW, game.startedAt)
        assertNull(game.completedAt)
        assertEquals(0L, game.version)
    }

    @Test
    fun `isOwnedBy compara com o dono`() {
        val game = Fixtures.game(userId = "u-1")

        assertTrue(game.isOwnedBy(UserId("u-1")))
        assertFalse(game.isOwnedBy(UserId("u-2")))
        assertFalse(game.isOwnedBy(UserId("")))
    }

    @Test
    fun `answered indica quais perguntas ja foram respondidas`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD))

        assertFalse(game.answered(0))
        assertFalse(game.answered(1))
        assertFalse(game.answered(2))

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertTrue(game.answered(0))
        assertFalse(game.answered(1))
        assertFalse(game.answered(2))
        assertFalse(game.answered(99))
    }

    @Test
    fun `questions e answers refletem o estado interno`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))

        assertEquals(2, game.questions.size)
        assertEquals(0, game.answers.size)

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertEquals(1, game.answers.size)
        assertEquals(0, game.answers.first().questionIndex)
        assertEquals(2, game.questions.size)
    }

    @Test
    fun `remainingQuestions diminui a cada resposta`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD))

        assertEquals(3, game.remainingQuestions)

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        assertEquals(2, game.remainingQuestions)

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        assertEquals(1, game.remainingQuestions)

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        assertEquals(0, game.remainingQuestions)
    }

    @Test
    fun `acumula pontos e acertos ao longo da partida`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.VERY_EASY, Difficulty.MASTER))
        val expected = Difficulty.VERY_EASY.points + Difficulty.MASTER.points

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertEquals(expected, game.score)
        assertEquals(2, game.correctAnswers)
        assertEquals(0, game.wrongAnswers)
        assertTrue(game.isCompleted)
    }

    @Test
    fun `acumula erros sem pontuar`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.HARD, Difficulty.HARD))

        repeat(2) {
            val question = game.currentQuestion()
            game.answer(question.options.first { it.id != question.correctOption }.id, Fixtures.NOW)
        }

        assertEquals(0, game.score)
        assertEquals(0, game.correctAnswers)
        assertEquals(2, game.wrongAnswers)
        assertTrue(game.isCompleted)
    }

    @Test
    fun `mistura de acerto e erro pontua apenas o acerto`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.EXPERT))

        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        val second = game.currentQuestion()
        game.answer(second.options.first { it.id != second.correctOption }.id, Fixtures.NOW)

        assertEquals(Difficulty.EASY.points, game.score)
        assertEquals(1, game.correctAnswers)
        assertEquals(1, game.wrongAnswers)
    }

    @Test
    fun `currentQuestion falha quando a partida ja terminou`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        assertThrows<DomainException> { game.currentQuestion() }
    }

    @Test
    fun `currentQuestion falha quando a partida foi abandonada`() {
        val game = Fixtures.game()
        game.abandon(Fixtures.NOW)

        assertThrows<DomainException> { game.currentQuestion() }
    }

    @Test
    fun `abandonar registra a data de conclusao`() {
        val game = Fixtures.game()
        val abandonedAt = Fixtures.NOW.plusSeconds(120)

        game.abandon(abandonedAt)

        assertEquals(GameStatus.ABANDONED, game.status)
        assertEquals(abandonedAt, game.completedAt)
        assertFalse(game.isCompleted)
        assertFalse(game.isInProgress)
    }

    @Test
    fun `abandonar duas vezes falha`() {
        val game = Fixtures.game()
        game.abandon(Fixtures.NOW)

        assertThrows<DomainException> { game.abandon(Fixtures.NOW.plusSeconds(1)) }
    }

    @Test
    fun `state expoe o estado completo`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", username = "alice")
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        val state = game.state()

        assertEquals(GameId("g-1"), state.id)
        assertEquals(UserId("u-1"), state.userId)
        assertEquals("alice", state.username)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(1, state.currentQuestionIndex)
        assertEquals(3, state.questions.size)
        assertEquals(1, state.answers.size)
        assertEquals(Difficulty.EASY.points, state.score)
        assertEquals(1, state.correctAnswers)
        assertEquals(0, state.wrongAnswers)
        assertEquals(0L, state.version)
    }

    @Test
    fun `reconstitute preserva partida concluida`() {
        val original = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))
        original.answer(original.currentQuestion().correctOption, Fixtures.NOW)
        original.answer(original.currentQuestion().correctOption, Fixtures.NOW)

        val restored = Game.reconstitute(original.state())

        assertEquals(GameStatus.COMPLETED, restored.status)
        assertEquals(original.score, restored.score)
        assertEquals(original.answers.size, restored.answers.size)
        assertEquals(Fixtures.NOW, restored.completedAt)
        assertThrows<DomainException> { restored.answer(OptionId("opt-0"), Fixtures.NOW) }
    }

    @Test
    fun `reconstitute preserva partida abandonada`() {
        val original = Fixtures.game()
        original.abandon(Fixtures.NOW)

        val restored = Game.reconstitute(original.state())

        assertEquals(GameStatus.ABANDONED, restored.status)
        assertFalse(restored.isInProgress)
        assertEquals(0, restored.answers.size)
    }

    @Test
    fun `reconstitute com versao customizada preserva o numero`() {
        val state = Fixtures.game().state().copy(version = 42L)

        assertEquals(42L, Game.reconstitute(state).version)
    }

    @Test
    fun `requireOwner aceita o dono e recusa terceiros`() {
        val game = Fixtures.game(userId = "u-1")

        game.requireOwner(UserId("u-1"))

        assertThrows<com.codelong.domain.exception.DomainException> {
            game.requireOwner(UserId("u-2"))
        }
    }

    @Test
    fun `cada resposta registra o indice e a alternativa escolhida`() {
        val game = Fixtures.game(difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))

        game.answer(OptionId("opt-1"), Fixtures.NOW)

        val record = game.answers.first()
        assertEquals(0, record.questionIndex)
        assertEquals(OptionId("opt-1"), record.chosenOption)
        assertEquals(game.questions.first().id, record.questionId)
    }
}
