package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetGameUseCaseTest {

    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: GetGameUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = GetGameUseCaseImpl(repository)
    }

    @Test
    fun `devolve a partida do dono`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", username = "alice")
        repository.save(game)

        val found = useCase.get(GameId("g-1"), UserId("u-1"))

        assertEquals(GameId("g-1"), found.id)
        assertEquals(UserId("u-1"), found.userId)
        assertEquals("alice", found.username)
        assertEquals(GameStatus.IN_PROGRESS, found.status())
    }

    @Test
    fun `devolve a instancia armazenada`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1")
        repository.save(game)

        assertSame(repository.findById(GameId("g-1")), useCase.get(GameId("g-1"), UserId("u-1")))
    }

    @Test
    fun `partida inexistente gera erro`() {
        val error = assertThrows<DomainException> {
            useCase.get(GameId("nao-existe"), UserId("u-1"))
        }

        assertEquals("GAME_NOT_FOUND", error.code)
        assertEquals("Game not found", error.message)
    }

    @Test
    fun `outro usuario nao acessa a partida`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val error = assertThrows<DomainException> {
            useCase.get(GameId("g-1"), UserId("u-2"))
        }

        assertEquals("GAME_ACCESS_DENIED", error.code)
    }

    @Test
    fun `devolve partida concluida para consulta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        repository.save(game)

        val found = useCase.get(GameId("g-1"), UserId("u-1"))

        assertEquals(GameStatus.COMPLETED, found.status())
        assertEquals(Difficulty.EASY.points, found.score())
    }

    @Test
    fun `devolve partida abandonada para consulta`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1")
        game.abandon(Fixtures.NOW)
        repository.save(game)

        assertEquals(GameStatus.ABANDONED, useCase.get(GameId("g-1"), UserId("u-1")).status())
    }

    @Test
    fun `nao vaza partida de outro usuario mesmo existindo`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))
        repository.save(Fixtures.game(id = "g-2", userId = "u-2"))

        assertThrows<DomainException> { useCase.get(GameId("g-2"), UserId("u-1")) }
    }
}
