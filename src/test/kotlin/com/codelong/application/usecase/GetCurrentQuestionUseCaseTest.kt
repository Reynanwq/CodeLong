package com.codelong.application.usecase

import com.codelong.domain.exception.ConflictException
import com.codelong.domain.exception.ForbiddenException
import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetCurrentQuestionUseCaseTest {

    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: GetCurrentQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = GetCurrentQuestionUseCase(repository)
    }

    @Test
    fun `devolve a pergunta atual do dono`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        repository.save(game)

        val question = useCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(game.questions().first().id, question.id)
        assertEquals(game.questions().first().statement, question.statement)
        assertEquals(Difficulty.EASY, question.difficulty)
    }

    @Test
    fun `nao expoe a resposta correta nem a explicacao`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val question = useCase.current(GameId("g-1"), UserId("u-1"))

        assertEquals(3, question.options.size)
        assertFalse(question.options.isEmpty())
        assertTrue(question.statement.isNotBlank())
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

        assertFalse(first.id == second.id)
        assertEquals(Difficulty.HARD, second.difficulty)
    }

    @Test
    fun `partida inexistente gera erro`() {
        val error = assertThrows<NotFoundException> {
            useCase.current(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_NOT_FOUND", error.code)
    }

    @Test
    fun `outro usuario nao acessa a pergunta`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val error = assertThrows<ForbiddenException> {
            useCase.current(GameId("g-1"), UserId("u-2"))
        }

        assertEquals("GAME_ACCESS_DENIED", error.code)
    }

    @Test
    fun `partida finalizada nao devolve pergunta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        repository.save(game)

        val error = assertThrows<ConflictException> {
            useCase.current(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_FINISHED", error.code)
    }

    @Test
    fun `partida abandonada nao devolve pergunta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1")
        game.abandon(Fixtures.NOW)
        repository.save(game)

        assertThrows<ConflictException> { useCase.current(GameId("g-1"), UserId("u-1")) }
    }
}
