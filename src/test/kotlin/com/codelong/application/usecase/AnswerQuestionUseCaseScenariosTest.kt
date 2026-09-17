package com.codelong.application.usecase

import com.codelong.domain.GameRules
import com.codelong.domain.exception.DomainException

import com.codelong.application.command.AnswerQuestionCommand
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
import java.time.Clock
import java.time.ZoneOffset

class AnswerQuestionUseCaseScenariosTest {

    private val actor = UserId("u-1")
    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: AnswerQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = AnswerQuestionUseCaseImpl(repository, TestClock.fixed)
    }

    private fun wrongOptionOf(game: com.codelong.domain.model.Game): OptionId {
        val question = game.currentQuestion()
        return question.options.first { it.id != question.correctOption }.id
    }

    @Test
    fun `resposta incorreta nao pontua e conta erro`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.HARD, Difficulty.HARD))
        )

        val result = useCase.answer(AnswerQuestionCommand(game.id, wrongOptionOf(game)), actor)

        assertEquals(0, result.currentScore)
        assertEquals(0, result.correctAnswers)
        assertEquals(1, result.wrongAnswers)
        assertFalse(result.record.correct)
        assertFalse(result.gameCompleted)
        assertNotNull(result.nextQuestion)
    }

    @Test
    fun `nextQuestion e a pergunta seguinte da sequencia`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )
        val expectedNext = game.questions[1]

        val result = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        assertEquals(expectedNext.id, result.nextQuestion?.id)
        assertEquals(expectedNext.statement, result.nextQuestion?.statement)
        assertEquals(expectedNext.options, result.nextQuestion?.options)
        assertEquals(expectedNext.difficulty, result.nextQuestion?.difficulty)
    }

    @Test
    fun `nextQuestion nao expoe a resposta correta`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))
        )

        val result = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        assertEquals(3, result.nextQuestion?.options?.size)
        assertEquals(false, result.nextQuestion?.options?.any { it.text.isBlank() })
    }

    @Test
    fun `percorre a partida inteira acumulando pontuacao`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.VERY_EASY, Difficulty.MEDIUM, Difficulty.MASTER))
        )
        val expected = Difficulty.VERY_EASY.points + Difficulty.MEDIUM.points + Difficulty.MASTER.points

        val first = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)
        assertEquals(Difficulty.VERY_EASY.points, first.currentScore)
        assertEquals(0, first.questionIndex)

        val second = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)
        assertEquals(Difficulty.VERY_EASY.points + Difficulty.MEDIUM.points, second.currentScore)
        assertEquals(1, second.questionIndex)
        assertFalse(second.gameCompleted)

        val third = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)
        assertEquals(expected, third.currentScore)
        assertEquals(2, third.questionIndex)
        assertEquals(3, third.correctAnswers)
        assertTrue(third.gameCompleted)
        assertNull(third.nextQuestion)
    }

    @Test
    fun `mistura acertos e erros ao longo da partida`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.EASY, Difficulty.EASY))
        )

        useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)
        useCase.answer(AnswerQuestionCommand(game.id, wrongOptionOf(game)), actor)
        val last = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        assertEquals(Difficulty.EASY.points * 2, last.currentScore)
        assertEquals(2, last.correctAnswers)
        assertEquals(1, last.wrongAnswers)
        assertEquals(3, last.totalQuestions)
    }

    @Test
    fun `nao aceita resposta apos concluir a partida`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY))
        )
        useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        val error = assertThrows<DomainException> {
            useCase.answer(AnswerQuestionCommand(game.id, OptionId("opt-0")), actor)
        }

        assertEquals("GAME_FINISHED", error.code)
    }

    @Test
    fun `nao aceita resposta em partida abandonada`() {
        val game = repository.save(Fixtures.game(userId = "u-1"))
        game.abandon(TestClock.fixed.instant())
        repository.save(game)

        val error = assertThrows<DomainException> {
            useCase.answer(AnswerQuestionCommand(game.id, OptionId("opt-0")), actor)
        }

        assertEquals("GAME_FINISHED", error.code)
    }

    @Test
    fun `registra o indice e o id da pergunta respondida`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )
        val firstQuestion = game.currentQuestion()

        val result = useCase.answer(AnswerQuestionCommand(game.id, firstQuestion.correctOption), actor)

        assertEquals(0, result.record.questionIndex)
        assertEquals(firstQuestion.id, result.record.questionId)
        assertEquals(firstQuestion.correctOption, result.record.chosenOption)
        assertEquals(Difficulty.EASY.points, result.record.earnedPoints)
    }

    @Test
    fun `resposta incorreta registra zero pontos no historico`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.MASTER, Difficulty.MASTER))
        )

        val result = useCase.answer(AnswerQuestionCommand(game.id, wrongOptionOf(game)), actor)

        assertEquals(0, result.record.earnedPoints)
        assertEquals(1, repository.findById(GameId("g-1"))!!.answers.size)
    }

    @Test
    fun `persiste o estado apos cada resposta`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))
        )

        useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        val stored = repository.findById(game.id)!!
        assertEquals(1, stored.currentQuestionIndex)
        assertEquals(1, stored.answers.size)
        assertTrue(stored.isInProgress)
    }

    @Test
    fun `resposta depois do prazo e rejeitada e a pergunta conta como erro`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )
        val afterDeadline = Fixtures.NOW.plusSeconds(GameRules.ANSWER_TIME_LIMIT_SECONDS + 1)
        val lateUseCase = AnswerQuestionUseCaseImpl(repository, Clock.fixed(afterDeadline, ZoneOffset.UTC))

        val error = assertThrows<DomainException> {
            lateUseCase.answer(
                AnswerQuestionCommand(game.id, game.currentQuestion().correctOption),
                actor
            )
        }

        assertEquals("ANSWER_TIME_EXPIRED", error.code)

        val stored = repository.findById(game.id)!!
        assertEquals(1, stored.currentQuestionIndex)
        assertEquals(0, stored.score)
        assertEquals(0, stored.correctAnswers)
        assertEquals(1, stored.wrongAnswers)
        assertTrue(stored.answers.single().timedOut)
        assertNull(stored.answers.single().chosenOption)
    }

    @Test
    fun `prazo renovado apos resposta permite responder a proxima`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )

        val first = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        assertNotNull(first.nextQuestionDeadline)
        assertEquals(
            TestClock.fixed.instant().plus(GameRules.ANSWER_TIME_LIMIT),
            first.nextQuestionDeadline
        )
    }

    @Test
    fun `ultima resposta nao devolve prazo seguinte`() {
        val game = repository.save(
            Fixtures.game(userId = "u-1", difficulties = listOf(Difficulty.EASY))
        )

        val result = useCase.answer(AnswerQuestionCommand(game.id, game.currentQuestion().correctOption), actor)

        assertNull(result.nextQuestion)
        assertNull(result.nextQuestionDeadline)
    }
}