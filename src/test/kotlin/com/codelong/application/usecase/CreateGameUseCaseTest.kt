package com.codelong.application.usecase

import com.codelong.application.service.DefaultGameFactory

import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.DomainException
import com.codelong.domain.service.GameSequencer
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class CreateGameUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var questionRepository: InMemoryQuestionRepository
    private lateinit var gameRepository: InMemoryGameRepository
    private lateinit var useCase: CreateGameUseCase

    @BeforeEach
    fun setUp() {
        userRepository = InMemoryUserRepository()
        questionRepository = InMemoryQuestionRepository()
        gameRepository = InMemoryGameRepository()
        useCase = CreateGameUseCaseImpl(
            userRepository = userRepository,
            questionRepository = questionRepository,
            gameRepository = gameRepository,
            gameFactory = DefaultGameFactory(GameSequencer(Random(1)), TestClock.fixed)
        )
    }

    @Test
    fun `inicia partida com o snapshot das perguntas ativas`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))
        questionRepository.save(Fixtures.question(id = "q-2", difficulty = Difficulty.HARD))

        val result = useCase.create(UserId("u-1"), GameMode.CLASSIC)
        val game = result.game

        assertTrue(result.created)
        assertTrue(game.isInProgress)
        assertEquals(UserId("u-1"), game.userId)
        assertEquals(2, game.totalQuestions)
        assertEquals(0, game.currentQuestionIndex)
        assertEquals(1L, game.version)
        assertEquals(1, gameRepository.all().size)
    }

    @Test
    fun `retoma a partida em andamento em vez de criar outra`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))
        val first = useCase.create(UserId("u-1"), GameMode.CLASSIC)

        val second = useCase.create(UserId("u-1"), GameMode.CLASSIC)

        assertFalse(second.created)
        assertEquals(first.game.id, second.game.id)
        assertEquals(1, gameRepository.all().size)
    }

    @Test
    fun `ignora perguntas inativas ao montar a sequencia`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "q-ativa", difficulty = Difficulty.EASY))
        questionRepository.save(
            Fixtures.question(id = "q-inativa", difficulty = Difficulty.EASY)
                .deactivate(TestClock.fixed.instant())
        )

        val game = useCase.create(UserId("u-1"), GameMode.CLASSIC).game

        assertEquals(1, game.totalQuestions)
    }

    @Test
    fun `sem perguntas ativas nao inicia partida`() {
        userRepository.save(Fixtures.user(id = "u-1"))

        val error = assertThrows<DomainException> { useCase.create(UserId("u-1"), GameMode.CLASSIC) }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `usuario inexistente nao inicia partida`() {
        questionRepository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.create(UserId("ninguem"), GameMode.CLASSIC) }
    }

    @Test
    fun `usuario inativo nao inicia partida`() {
        userRepository.save(Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant()))
        questionRepository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.create(UserId("u-1"), GameMode.CLASSIC) }
    }
}
