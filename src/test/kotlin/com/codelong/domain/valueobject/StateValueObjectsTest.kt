package com.codelong.domain.valueobject

import com.codelong.domain.valueobject.GameMode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class StateValueObjectsTest {

    private val now: Instant = Instant.parse("2026-01-01T12:00:00Z")
    private val later: Instant = Instant.parse("2026-01-01T13:00:00Z")

    private val option = QuestionOption(OptionId("a"), "Alternativa A")

    private val gameQuestion = GameQuestion(
        id = QuestionId("q-1"),
        statement = "Pergunta?",
        options = listOf(option),
        correctOption = OptionId("a"),
        explanation = "Explicacao.",
        category = Category.OOP,
        difficulty = Difficulty.EASY
    )

    private val answerRecord = AnswerRecord(
        questionIndex = 0,
        questionId = QuestionId("q-1"),
        chosenOption = OptionId("a"),
        correct = true,
        earnedPoints = 20,
        answeredAt = now
    )

    @Test
    fun `UserProfile withPasswordHash troca apenas o hash`() {
        val profile = UserProfile(
            username = Username.of("alice"),
            email = Email.of("alice@codelong.dev"),
            passwordHash = PasswordHash("hash-antigo"),
            role = Role.USER
        )

        val updated = profile.withPasswordHash(PasswordHash("hash-novo"))

        assertEquals(PasswordHash("hash-novo"), updated.passwordHash)
        assertEquals(profile.username, updated.username)
        assertEquals(profile.email, updated.email)
        assertEquals(profile.role, updated.role)
        assertEquals(PasswordHash("hash-antigo"), profile.passwordHash)
        assertFalse(profile == updated)
    }

    @Test
    fun `UserProfile tem igualdade por valor`() {
        val first = UserProfile(
            Username.of("alice"),
            Email.of("alice@codelong.dev"),
            PasswordHash("hash"),
            Role.ADMIN
        )
        val second = UserProfile(
            Username.of("alice"),
            Email.of("alice@codelong.dev"),
            PasswordHash("hash"),
            Role.ADMIN
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `UserState agrupa identidade perfil e datas`() {
        val profile = UserProfile(
            Username.of("alice"),
            Email.of("alice@codelong.dev"),
            PasswordHash("hash"),
            Role.USER
        )
        val state = UserState(
            id = UserId("u-1"),
            profile = profile,
            status = AccountStatus.ACTIVE,
            createdAt = now,
            updatedAt = later
        )

        assertEquals(UserId("u-1"), state.id)
        assertEquals(profile, state.profile)
        assertEquals(AccountStatus.ACTIVE, state.status)
        assertEquals(now, state.createdAt)
        assertEquals(later, state.updatedAt)
        assertEquals(state, state.copy(status = AccountStatus.INACTIVE).copy(status = AccountStatus.ACTIVE))
    }

    @Test
    fun `QuestionState agrupa conteudo status e datas`() {
        val content = QuestionContent(
            statement = "Pergunta?",
            options = listOf(option, QuestionOption(OptionId("b"), "Alternativa B")),
            correctOption = OptionId("a"),
            explanation = "Explicacao.",
            category = Category.KOTLIN,
            difficulty = Difficulty.HARD
        )
        val state = QuestionState(
            id = QuestionId("q-1"),
            content = content,
            status = QuestionStatus.ACTIVE,
            createdAt = now,
            updatedAt = later
        )

        assertEquals(QuestionId("q-1"), state.id)
        assertEquals(content, state.content)
        assertEquals(QuestionStatus.ACTIVE, state.status)
        assertEquals(state, state.copy(status = QuestionStatus.INACTIVE).copy(status = QuestionStatus.ACTIVE))
    }

    @Test
    fun `GameState carrega todos os campos da partida`() {
        val state = GameState(mode = GameMode.CLASSIC, 
            id = GameId("g-1"),
            userId = UserId("u-1"),
            username = "alice",
            status = GameStatus.IN_PROGRESS,
            startedAt = now,
            completedAt = null,
            currentQuestionIndex = 0,
            currentQuestionDeadline = later,
            questions = listOf(gameQuestion),
            answers = listOf(answerRecord),
            score = 20,
            correctAnswers = 1,
            wrongAnswers = 0,
            version = 3L
        )

        assertEquals(GameId("g-1"), state.id)
        assertEquals(UserId("u-1"), state.userId)
        assertEquals("alice", state.username)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertNull(state.completedAt)
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(1, state.questions.size)
        assertEquals(1, state.answers.size)
        assertEquals(20, state.score)
        assertEquals(1, state.correctAnswers)
        assertEquals(0, state.wrongAnswers)
        assertEquals(3L, state.version)
    }

    @Test
    fun `GameState copy altera somente o campo informado`() {
        val state = GameState(mode = GameMode.CLASSIC, 
            id = GameId("g-1"),
            userId = UserId("u-1"),
            username = "alice",
            status = GameStatus.IN_PROGRESS,
            startedAt = now,
            completedAt = null,
            currentQuestionIndex = 0,
            currentQuestionDeadline = later,
            questions = listOf(gameQuestion),
            answers = emptyList(),
            score = 0,
            correctAnswers = 0,
            wrongAnswers = 0,
            version = 1L
        )

        val completed = state.copy(
            status = GameStatus.COMPLETED,
            completedAt = later,
            currentQuestionIndex = 1,
            score = 20,
            correctAnswers = 1
        )

        assertEquals(GameStatus.COMPLETED, completed.status)
        assertEquals(later, completed.completedAt)
        assertEquals(1, completed.currentQuestionIndex)
        assertEquals(20, completed.score)
        assertEquals(1L, completed.version)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
    }

    @Test
    fun `RankEntry position e opcional`() {
        val withoutPosition = RankEntry(
            userId = UserId("u-1"),
            username = "alice",
            score = 100,
            correctAnswers = 3,
            answeredQuestions = 10,
            totalTimeMillis = 5_000,
            achievedAt = now
        )
        val withPosition = withoutPosition.copy(position = 1)

        assertNull(withoutPosition.position)
        assertEquals(1, withPosition.position)
        assertEquals(100, withPosition.score)
        assertEquals(withoutPosition, withPosition.copy(position = null))
    }

    @Test
    fun `AnswerRecord carrega o resultado da resposta`() {
        assertEquals(0, answerRecord.questionIndex)
        assertEquals(QuestionId("q-1"), answerRecord.questionId)
        assertEquals(OptionId("a"), answerRecord.chosenOption)
        assertEquals(true, answerRecord.correct)
        assertEquals(20, answerRecord.earnedPoints)
        assertEquals(now, answerRecord.answeredAt)
    }

    @Test
    fun `AnswerEval agrupa avaliacao e progresso`() {
        val eval = AnswerEval(
            record = answerRecord,
            question = gameQuestion,
            currentScore = 20,
            correctAnswers = 1,
            wrongAnswers = 0,
            gameCompleted = false,
            questionIndex = 0,
            totalQuestions = 2
        )

        assertEquals(answerRecord, eval.record)
        assertEquals(gameQuestion, eval.question)
        assertEquals(20, eval.currentScore)
        assertEquals(1, eval.correctAnswers)
        assertEquals(0, eval.wrongAnswers)
        assertFalse(eval.gameCompleted)
        assertEquals(0, eval.questionIndex)
        assertEquals(2, eval.totalQuestions)
    }

    @Test
    fun `AnswerResult inclui a proxima pergunta quando existir`() {
        val next = gameQuestion.copy(id = QuestionId("q-2"))
        val result = AnswerResult(
            record = answerRecord,
            question = gameQuestion,
            currentScore = 20,
            correctAnswers = 1,
            wrongAnswers = 0,
            gameCompleted = false,
            questionIndex = 0,
            totalQuestions = 2,
            nextQuestion = next.publicView(),
            nextQuestionDeadline = later
        )

        assertEquals(QuestionId("q-2"), result.nextQuestion?.id)
        assertEquals(2, result.totalQuestions)
    }

    @Test
    fun `AnswerResult nextQuestion pode ser nulo na ultima pergunta`() {
        val result = AnswerResult(
            record = answerRecord,
            question = gameQuestion,
            currentScore = 20,
            correctAnswers = 1,
            wrongAnswers = 0,
            gameCompleted = true,
            questionIndex = 1,
            totalQuestions = 2,
            nextQuestion = null,
            nextQuestionDeadline = null
        )

        assertNull(result.nextQuestion)
        assertEquals(true, result.gameCompleted)
    }

    @Test
    fun `TokenClaims carrega usuario papel e validade`() {
        val claims = TokenClaims(
            userId = UserId("u-1"),
            role = Role.ADMIN,
            issuedAt = now,
            expiresAt = later
        )

        assertEquals(UserId("u-1"), claims.userId)
        assertEquals(Role.ADMIN, claims.role)
        assertEquals(now, claims.issuedAt)
        assertEquals(later, claims.expiresAt)
        assertEquals(claims, claims.copy())
    }

    @Test
    fun `QuestionOption preserva id e texto`() {
        val option = QuestionOption(OptionId("opt-0"), "Texto")

        assertEquals(OptionId("opt-0"), option.id)
        assertEquals("Texto", option.text)
        assertEquals(option, QuestionOption(OptionId("opt-0"), "Texto"))
        assertEquals(option.id, option.copy().id)
    }
}
