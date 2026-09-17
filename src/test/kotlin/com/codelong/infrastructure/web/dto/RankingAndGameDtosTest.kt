package com.codelong.infrastructure.web.dto

import com.codelong.application.result.RankingPage
import com.codelong.domain.valueobject.AnswerRecord
import com.codelong.domain.valueobject.AnswerResult
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RankingAndGameDtosTest {

    private fun rankEntry(
        id: String = "u-1",
        position: Int? = null,
        score: Int = 100
    ) = RankEntry(
        userId = UserId(id),
        username = id,
        score = score,
        correctAnswers = 3,
        totalTimeMillis = 5_000,
        achievedAt = Fixtures.NOW,
        position = position
    )

    @Test
    fun `RankingEntryResponse converte a entrada com posicao`() {
        val response = RankingEntryResponse.from(rankEntry(position = 1))

        assertEquals(1, response.position)
        assertEquals("u-1", response.userId)
        assertEquals("u-1", response.username)
        assertEquals(100, response.score)
        assertEquals(3, response.correctAnswers)
        assertEquals(5_000L, response.totalTimeMillis)
        assertEquals(Fixtures.NOW, response.achievedAt)
    }

    @Test
    fun `RankingEntryResponse converte a entrada sem posicao`() {
        assertNull(RankingEntryResponse.from(rankEntry()).position)
    }

    @Test
    fun `RankingResponse calcula o total de paginas`() {
        val page = RankingPage(entries = listOf(rankEntry(position = 1)), totalElements = 25L, page = 0, size = 10)

        val response = RankingResponse.from(page)

        assertEquals(3, response.totalPages)
        assertEquals(25L, response.totalElements)
        assertEquals(1, response.entries.size)
        assertEquals(0, response.page)
        assertEquals(10, response.size)
    }

    @Test
    fun `RankingResponse com total exato nao adiciona pagina extra`() {
        val page = RankingPage(entries = emptyList(), totalElements = 20L, page = 0, size = 10)

        assertEquals(2, RankingResponse.from(page).totalPages)
    }

    @Test
    fun `RankingResponse com size zero resulta em zero paginas`() {
        val page = RankingPage(entries = emptyList(), totalElements = 10L, page = 0, size = 0)

        assertEquals(0, RankingResponse.from(page).totalPages)
    }

    @Test
    fun `RankingResponse com lista vazia e valido`() {
        val page = RankingPage(entries = emptyList(), totalElements = 0L, page = 0, size = 20)

        val response = RankingResponse.from(page)

        assertTrue(response.entries.isEmpty())
        assertEquals(0, response.totalPages)
    }

    @Test
    fun `PageResponse calcula o total de paginas arredondando para cima`() {
        val response = PageResponse.of(items = listOf("a"), totalElements = 21L, page = 0, size = 10)

        assertEquals(3, response.totalPages)
        assertEquals(1, response.items.size)
        assertEquals(21L, response.totalElements)
    }

    @Test
    fun `PageResponse com size zero resulta em zero paginas`() {
        val response = PageResponse.of(items = emptyList<String>(), totalElements = 5L, page = 0, size = 0)

        assertEquals(0, response.totalPages)
    }

    @Test
    fun `PageResponse com size negativo resulta em zero paginas`() {
        val response = PageResponse.of(items = emptyList<String>(), totalElements = 5L, page = 0, size = -1)

        assertEquals(0, response.totalPages)
    }

    @Test
    fun `PublicQuestionResponse converte a visao publica`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.HARD, category = Category.REST)

        val response = PublicQuestionResponse.from(question.snapshot().publicView())

        assertEquals("q-1", response.id)
        assertEquals(question.statement, response.statement)
        assertEquals(3, response.options.size)
        assertEquals("REST", response.category)
        assertEquals("HARD", response.difficulty)
    }

    @Test
    fun `GameResponse converte partida em andamento`() {
        val game = Fixtures.game(id = "g-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))

        val response = GameResponse.from(game)

        assertEquals("g-1", response.id)
        assertEquals("IN_PROGRESS", response.status)
        assertEquals(0, response.currentQuestionIndex)
        assertEquals(2, response.totalQuestions)
        assertEquals(2, response.remainingQuestions)
        assertEquals(0, response.score)
        assertEquals(0, response.correctAnswers)
        assertEquals(0, response.wrongAnswers)
        assertEquals(Fixtures.NOW, response.startedAt)
        assertNull(response.completedAt)
    }

    @Test
    fun `GameResponse converte partida concluida`() {
        val game = Fixtures.game(id = "g-1", difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)

        val response = GameResponse.from(game)

        assertEquals("COMPLETED", response.status)
        assertEquals(1, response.currentQuestionIndex)
        assertEquals(0, response.remainingQuestions)
        assertEquals(Difficulty.EASY.points, response.score)
        assertEquals(1, response.correctAnswers)
        assertEquals(0, response.wrongAnswers)
        assertEquals(Fixtures.NOW, response.completedAt)
    }

    @Test
    fun `GameResponse converte partida abandonada`() {
        val game = Fixtures.game(id = "g-1")
        game.abandon(Fixtures.NOW)

        val response = GameResponse.from(game)

        assertEquals("ABANDONED", response.status)
        assertEquals(Fixtures.NOW, response.completedAt)
    }

    @Test
    fun `AnswerRequest carrega a opcao escolhida`() {
        assertEquals("opt-0", AnswerRequest("opt-0").optionId)
    }

    private fun answerResult(nextQuestion: com.codelong.domain.valueobject.QuestionPublic?) = AnswerResult(
        record = AnswerRecord(
            questionIndex = 0,
            questionId = QuestionId("q-1"),
            chosenOption = OptionId("a"),
            correct = true,
            earnedPoints = 20,
            answeredAt = Fixtures.NOW
        ),
        question = Fixtures.question(id = "q-1").snapshot(),
        currentScore = 20,
        correctAnswers = 1,
        wrongAnswers = 0,
        gameCompleted = nextQuestion == null,
        questionIndex = 0,
        totalQuestions = 2,
        nextQuestion = nextQuestion,
        nextQuestionDeadline = nextQuestion?.let { Fixtures.NOW.plusSeconds(20) }
    )

    @Test
    fun `AnswerResponse converte resultado com proxima pergunta`() {
        val next = Fixtures.question(id = "q-2", difficulty = Difficulty.HARD).snapshot().publicView()

        val response = AnswerResponse.from(answerResult(next))

        assertEquals(true, response.correct)
        assertEquals("a", response.chosenOption)
        assertEquals("opt-0", response.correctOption)
        assertEquals("Polimorfismo permite tratar objetos de tipos diferentes de forma uniforme.", response.explanation)
        assertEquals(20, response.earnedPoints)
        assertEquals(20, response.currentScore)
        assertEquals(1, response.correctAnswers)
        assertEquals(0, response.wrongAnswers)
        assertEquals(false, response.gameCompleted)
        assertEquals(0, response.questionIndex)
        assertEquals(2, response.totalQuestions)
        assertEquals("q-2", response.nextQuestion?.id)
    }

    @Test
    fun `AnswerResponse converte resultado sem proxima pergunta`() {
        val response = AnswerResponse.from(answerResult(null))

        assertEquals(true, response.gameCompleted)
        assertNull(response.nextQuestion)
    }

    @Test
    fun `OptionResponse dentro do PublicQuestionResponse preserva os ids`() {
        val options = listOf(
            QuestionOption(OptionId("a"), "Alternativa A"),
            QuestionOption(OptionId("b"), "Alternativa B")
        )

        assertEquals(listOf("a", "b"), options.map { OptionResponse.from(it).id })
    }

    @Test
    fun `PublicQuestionResponse carrega prazo e limite de tempo`() {
        val question = Fixtures.question(id = "q-1")

        val response = PublicQuestionResponse.from(question.snapshot().publicView(), Fixtures.NOW.plusSeconds(20))

        assertEquals(Fixtures.NOW.plusSeconds(20), response.deadline)
        assertEquals(20L, response.timeLimitSeconds)
    }

    @Test
    fun `PublicQuestionResponse sem prazo informado fica nulo`() {
        val response = PublicQuestionResponse.from(Fixtures.question(id = "q-1").snapshot().publicView())

        assertNull(response.deadline)
        assertEquals(20L, response.timeLimitSeconds)
    }

    @Test
    fun `GameResponse carrega o prazo da pergunta atual`() {
        val game = Fixtures.game(id = "g-1")

        val response = GameResponse.from(game)

        assertEquals(game.currentQuestionDeadline, response.currentQuestionDeadline)
    }

    @Test
    fun `AnswerResponse converte pergunta perdida por tempo`() {
        val record = AnswerRecord(
            questionIndex = 0,
            questionId = QuestionId("q-1"),
            chosenOption = null,
            correct = false,
            earnedPoints = 0,
            answeredAt = Fixtures.NOW,
            timedOut = true
        )
        val result = AnswerResult(
            record = record,
            question = Fixtures.question(id = "q-1").snapshot(),
            currentScore = 0,
            correctAnswers = 0,
            wrongAnswers = 1,
            gameCompleted = true,
            questionIndex = 0,
            totalQuestions = 1,
            nextQuestion = null,
            nextQuestionDeadline = null
        )

        val response = AnswerResponse.from(result)

        assertEquals(true, response.timedOut)
        assertNull(response.chosenOption)
        assertEquals(false, response.correct)
        assertEquals(0, response.earnedPoints)
        assertEquals(1, response.wrongAnswers)
        assertNull(response.nextQuestion)
    }
}