package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.infrastructure.persistence.document.AnswerDocument
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.GameQuestionDocument
import com.codelong.infrastructure.persistence.document.OptionDocument
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GamePersistenceMapperTest {

    @Test
    fun `toDocument copia todos os campos da partida`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", username = "alice")

        val document = GamePersistenceMapper.toDocument(game)

        assertEquals("g-1", document.id)
        assertEquals("u-1", document.userId)
        assertEquals("alice", document.username)
        assertEquals("IN_PROGRESS", document.status)
        assertEquals(Fixtures.NOW, document.startedAt)
        assertNull(document.completedAt)
        assertEquals(0, document.currentQuestionIndex)
        assertEquals(3, document.questions.size)
        assertEquals(0, document.answers.size)
        assertEquals(0, document.score)
        assertEquals(0, document.correctAnswers)
        assertEquals(0, document.wrongAnswers)
        assertEquals(0L, document.version)
    }

    @Test
    fun `toDocument converte o snapshot das perguntas`() {
        val game = Fixtures.game(id = "g-1")

        val document = GamePersistenceMapper.toDocument(game)

        val first = document.questions.first()
        assertEquals("q-0", first.id)
        assertEquals("O que e polimorfismo?", first.statement)
        assertEquals(listOf("opt-0", "opt-1", "opt-2"), first.options.map { it.id })
        assertEquals("opt-0", first.correctOption)
        assertEquals("OOP", first.category)
        assertEquals("EASY", first.difficulty)
    }

    @Test
    fun `toDomain reconstroi a partida`() {
        val document = GameDocument(
            id = "g-1",
            userId = "u-1",
            username = "alice",
            status = "IN_PROGRESS",
            startedAt = Fixtures.NOW,
            currentQuestionIndex = 0,
            questions = listOf(
                GameQuestionDocument(
                    id = "q-1",
                    statement = "Pergunta?",
                    options = listOf(OptionDocument("a", "A"), OptionDocument("b", "B")),
                    correctOption = "a",
                    explanation = "Explicacao.",
                    category = "OOP",
                    difficulty = "EASY"
                )
            ),
            version = 5L
        )

        val game = GamePersistenceMapper.toDomain(document)

        assertEquals("g-1", game.id.value)
        assertEquals("u-1", game.userId.value)
        assertEquals("alice", game.username)
        assertEquals(GameStatus.IN_PROGRESS, game.status)
        assertEquals(1, game.totalQuestions)
        assertEquals(OptionId("a"), game.currentQuestion().correctOption)
        assertEquals(5L, game.version)
    }

    @Test
    fun `round trip preserva partida em andamento com respostas`() {
        val original = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))
        original.answer(original.currentQuestion().correctOption, Fixtures.NOW)

        val restored = GamePersistenceMapper.toDomain(GamePersistenceMapper.toDocument(original))

        assertEquals(original.status, restored.status)
        assertEquals(original.score, restored.score)
        assertEquals(original.currentQuestionIndex, restored.currentQuestionIndex)
        assertEquals(original.answers, restored.answers)
        assertEquals(original.correctAnswers, restored.correctAnswers)
        assertEquals(original.wrongAnswers, restored.wrongAnswers)
    }

    @Test
    fun `round trip preserva partida concluida`() {
        val original = Fixtures.game(id = "g-1", difficulties = listOf(Difficulty.MASTER))
        original.answer(original.currentQuestion().correctOption, Fixtures.NOW)

        val restored = GamePersistenceMapper.toDomain(GamePersistenceMapper.toDocument(original))

        assertEquals(GameStatus.COMPLETED, restored.status)
        assertEquals(Fixtures.NOW, restored.completedAt)
        assertEquals(Difficulty.MASTER.points, restored.score)
        assertTrue(restored.isCompleted)
    }

    @Test
    fun `round trip preserva partida abandonada`() {
        val original = Fixtures.game(id = "g-1")
        original.abandon(Fixtures.NOW)

        val restored = GamePersistenceMapper.toDomain(GamePersistenceMapper.toDocument(original))

        assertEquals(GameStatus.ABANDONED, restored.status)
        assertFalse(restored.isInProgress)
    }

    @Test
    fun `round trip preserva a versao`() {
        val original = Fixtures.game(id = "g-1")
        original.version = 42L

        assertEquals(42L, GamePersistenceMapper.toDomain(GamePersistenceMapper.toDocument(original)).version)
    }

    @Test
    fun `toDomain converte as respostas registradas`() {
        val document = GameDocument(
            id = "g-1",
            userId = "u-1",
            username = "alice",
            status = "IN_PROGRESS",
            answers = listOf(
                AnswerDocument(
                    questionIndex = 0,
                    questionId = "q-1",
                    chosenOption = "a",
                    correct = true,
                    earnedPoints = 20,
                    answeredAt = Fixtures.NOW
                )
            )
        )

        val game = GamePersistenceMapper.toDomain(document)

        assertEquals(1, game.answers.size)
        assertEquals(0, game.answers.first().questionIndex)
        assertEquals(OptionId("a"), game.answers.first().chosenOption)
        assertTrue(game.answers.first().correct)
        assertEquals(20, game.answers.first().earnedPoints)
    }

    @Test
    fun `round trip de partida sem perguntas e valido`() {
        val original = Fixtures.game(id = "g-1")

        val document = GamePersistenceMapper.toDocument(original)
        val restored = GamePersistenceMapper.toDomain(document.copy(questions = emptyList()))

        assertEquals(0, restored.totalQuestions)
        assertTrue(restored.isInProgress)
    }
}
