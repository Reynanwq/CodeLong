package com.codelong.application.usecase

import com.codelong.application.command.AnswerQuestionCommand
import com.codelong.domain.exception.DomainException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnswerQuestionUseCaseTest {

    private val actor = UserId("u-1")
    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: AnswerQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = AnswerQuestionUseCase(repository, TestClock.fixed)
    }

    @Test
    fun `resposta correta pontua e retorna a proxima pergunta`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )

        val result = useCase.answer(
            AnswerQuestionCommand(game.id, game.currentQuestion().correctOption),
            actor
        )

        assertEquals(Difficulty.EASY.points, result.currentScore)
        assertEquals(1, result.correctAnswers)
        assertFalse(result.gameCompleted)
        assertNotNull(result.nextQuestion)
        assertTrue(result.record.correct)
    }

    @Test
    fun `responder a ultima pergunta finaliza a partida`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY))
        )

        val result = useCase.answer(
            AnswerQuestionCommand(game.id, game.currentQuestion().correctOption),
            actor
        )

        assertTrue(result.gameCompleted)
        assertNull(result.nextQuestion)
        assertEquals(0, result.questionIndex)
        assertEquals(1, result.totalQuestions)
        assertTrue(repository.findById(game.id)!!.isCompleted())
    }

    @Test
    fun `persiste o avanco da partida`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )

        useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        val stored = repository.findById(game.id)!!
        assertEquals(1, stored.currentQuestionIndex())
        assertEquals(Difficulty.EASY.points, stored.score())
    }

    @Test
    fun `partida inexistente resulta em NotFound`() {
        val error = assertThrows<DomainException> {
            useCase.answer(AnswerQuestionCommand(GameId("nao-existe"), OptionId("opt-0")), actor)
        }

        assertEquals("GAME_NOT_FOUND", error.code)
    }

    @Test
    fun `outro usuario nao responde pela partida`() {
        val game = repository.save(Fixtures.game(userId = "u-1"))

        assertThrows<DomainException> {
            useCase.answer(
                AnswerQuestionCommand(game.id, game.currentQuestion().correctOption),
                UserId("intruso")
            )
        }
    }

    @Test
    fun `opcao invalida e rejeitada`() {
        val game = repository.save(Fixtures.game(userId = "u-1"))

        assertThrows<DomainException> {
            useCase.answer(AnswerQuestionCommand(game.id, OptionId("opcao-inexistente")), actor)
        }
    }
}