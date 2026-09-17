package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AbandonGameUseCaseTest {

    private lateinit var repository: InMemoryGameRepository
    private lateinit var useCase: AbandonGameUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        useCase = AbandonGameUseCaseImpl(repository, TestClock.fixed)
    }

    @Test
    fun `abandona a partida do proprio usuario`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val abandoned = useCase.abandon(GameId("g-1"), UserId("u-1"))

        assertEquals(GameStatus.ABANDONED, abandoned.status())
        assertEquals(Fixtures.NOW, abandoned.completedAt())
        assertEquals("g-1", abandoned.id.value)
    }

    @Test
    fun `persiste o estado abandonado`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        useCase.abandon(GameId("g-1"), UserId("u-1"))

        val stored = repository.findById(GameId("g-1"))
        assertNotNull(stored)
        assertEquals(GameStatus.ABANDONED, stored!!.status())
        assertEquals(2L, stored.version)
    }

    @Test
    fun `partida inexistente gera erro`() {
        val error = assertThrows<DomainException> {
            useCase.abandon(GameId("nao-existe"), UserId("u-1"))
        }

        assertEquals("GAME_NOT_FOUND", error.code)
        assertEquals("Game not found", error.message)
    }

    @Test
    fun `outro usuario nao pode abandonar a partida`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))

        val error = assertThrows<DomainException> {
            useCase.abandon(GameId("g-1"), UserId("u-2"))
        }

        assertEquals("GAME_ACCESS_DENIED", error.code)
        assertEquals(GameStatus.IN_PROGRESS, repository.findById(GameId("g-1"))!!.status())
    }

    @Test
    fun `nao abandona partida ja abandonada`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))
        useCase.abandon(GameId("g-1"), UserId("u-1"))

        val error = assertThrows<DomainException> {
            useCase.abandon(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_FINISHED", error.code)
    }

    @Test
    fun `nao abandona partida concluida`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY))
        game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        repository.save(game)

        val error = assertThrows<DomainException> {
            useCase.abandon(GameId("g-1"), UserId("u-1"))
        }

        assertEquals("GAME_FINISHED", error.code)
        assertEquals(GameStatus.COMPLETED, repository.findById(GameId("g-1"))!!.status())
    }

    @Test
    fun `abandonar uma partida nao afeta as demais`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))
        repository.save(Fixtures.game(id = "g-2", userId = "u-1"))

        useCase.abandon(GameId("g-1"), UserId("u-1"))

        assertEquals(GameStatus.ABANDONED, repository.findById(GameId("g-1"))!!.status())
        assertEquals(GameStatus.IN_PROGRESS, repository.findById(GameId("g-2"))!!.status())
    }

    @Test
    fun `abandonar duas partidas do mesmo usuario e permitido`() {
        repository.save(Fixtures.game(id = "g-1", userId = "u-1"))
        repository.save(Fixtures.game(id = "g-2", userId = "u-1"))

        useCase.abandon(GameId("g-1"), UserId("u-1"))
        useCase.abandon(GameId("g-2"), UserId("u-1"))

        assertEquals(GameStatus.ABANDONED, repository.findById(GameId("g-1"))!!.status())
        assertEquals(GameStatus.ABANDONED, repository.findById(GameId("g-2"))!!.status())
    }
}
