package com.codelong.application.usecase

import com.codelong.domain.GameRules
import com.codelong.domain.exception.DomainException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.ZoneOffset

class GetCurrentQuestionUseCaseTest {

    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: GetCurrentQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = GetCurrentQuestionUseCaseImpl(repository, TestClock.fixed)
    }

    @Test
    fun `devolve a pergunta atual do dono`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        repository.save(game)

        val current = useCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(game.questions.first().id, current.question.id)
        assertEquals(game.questions.first().statement, current.question.statement)
        assertEquals(Difficulty.EASY, current.question.difficulty)
    }

    @Test
    fun `devolve posicao total e prazo da pergunta atual`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val current = useCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(0, current.index)
        assertEquals(3, current.total)
        assertEquals(Fixtures.NOW.plus(GameRules.ANSWER_TIME_LIMIT), current.deadline)
    }

    @Test
    fun `nao expoe a resposta correta nem a explicacao`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val current = useCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(3, current.question.options.size)
        assertFalse(current.question.options.isEmpty())
        assertTrue(current.question.statement.isNotBlank())
    }

    @Test
    fun `avanca para a proxima pergunta apos responder`() {
        repository.save(
            Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )

        val first = useCase.current(GameId("g-1"), UserId("u-1"))

        val stored = repository.findById(GameId("g-1"))!!
        stored.answer(stored.currentQuestion().correctOption, Fixtures.NOW)
        repository.save(stored)

        val second = useCase.current(GameId("g-1"), UserId("u-1"))

        assertFalse(first.question.id == second.question.id)
        assertEquals(Difficulty.HARD, second.question.difficulty)
    }

    @Test
    fun `pergunta expirada conta como erro e avanca para a proxima`() {
        repository.save(
            Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )
        val afterDeadline = Fixtures.NOW.plusSeconds(GameRules.ANSWER_TIME_LIMIT_SECONDS + 1)
        val lateUseCase = GetCurrentQuestionUseCaseImpl(
            repository,
            Clock.fixed(afterDeadline, ZoneOffset.UTC)
        )

        val current = lateUseCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(Difficulty.HARD, current.question.difficulty)
        assertEquals(1, current.index)
        assertEquals(afterDeadline.plus(GameRules.ANSWER_TIME_LIMIT), current.deadline)

        val stored = repository.findById(GameId("g-1"))!!
        assertEquals(1, stored.wrongAnswers)
        assertEquals(0, stored.score)
        assertTrue(stored.answers.single().timedOut)
        assertFalse(stored.answers.single().correct)
    }

    @Test
    fun `expiracao na ultima pergunta conclui a partida`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY)))
        val afterDeadline = Fixtures.NOW.plusSeconds(GameRules.ANSWER_TIME_LIMIT_SECONDS + 1)
        val lateUseCase = GetCurrentQuestionUseCaseImpl(
            repository,
            Clock.fixed(afterDeadline, ZoneOffset.UTC)
        )

        val error = assertThrows<DomainException> { lateUseCase.current(GameId("g-1"), UserId("u-1")) }

        assertEquals("GAME_FINISHED", error.code)
        assertEquals(1, repository.findById(GameId("g-1"))!!.wrongAnswers)
    }

    @Test
    fun `partida inexistente gera erro`() {
        val error = assertThrows<DomainException> {
            useCase.current(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_NOT_FOUND", error.code)
    }

    @Test
    fun `outro usuario nao acessa a pergunta`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val error = assertThrows<DomainException> {
            useCase.current(GameId("g-1"), UserId("u-2"))
        }

        assertEquals("GAME_ACCESS_DENIED", error.code)
    }

    @Test
    fun `partida finalizada nao devolve pergunta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        repository.save(game)

        val error = assertThrows<DomainException> {
            useCase.current(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_FINISHED", error.code)
    }

    @Test
    fun `partida abandonada nao devolve pergunta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1")
        game.abandon(Fixtures.NOW)
        repository.save(game)

        assertThrows<DomainException> { useCase.current(GameId("g-1"), UserId("u-1")) }
    }
}
