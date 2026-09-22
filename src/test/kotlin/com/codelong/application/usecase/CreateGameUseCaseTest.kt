package com.codelong.application.usecase

import com.codelong.application.service.DefaultGameFactory

import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.DomainException
import com.codelong.domain.service.GameSequencer
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.QuestionId
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

        val result = useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null)
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
        val first = useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null)

        val second = useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null)

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

        val game = useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null).game

        assertEquals(1, game.totalQuestions)
    }

    @Test
    fun `sem perguntas ativas nao inicia partida`() {
        userRepository.save(Fixtures.user(id = "u-1"))

        val error = assertThrows<DomainException> { useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null) }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `usuario inexistente nao inicia partida`() {
        questionRepository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.create(UserId("ninguem"), GameMode.CLASSIC, null, null) }
    }

    @Test
    fun `usuario inativo nao inicia partida`() {
        userRepository.save(Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant()))
        questionRepository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null) }
    }

    @Test
    fun `aprendizado inicia apenas com as perguntas do tema`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))
        questionRepository.save(Fixtures.question(id = "k-2", category = Category.KOTLIN))
        questionRepository.save(Fixtures.question(id = "r-1", category = Category.REST))

        val game = useCase.create(UserId("u-1"), GameMode.APRENDIZADO, Category.KOTLIN, null).game

        assertEquals(2, game.totalQuestions)
        assertTrue(game.questions.all { it.category == Category.KOTLIN })
    }

    @Test
    fun `aprendizado exige tema ou pergunta`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, null, null)
        }

        assertEquals("learning.theme.required", error.code)
    }

    @Test
    fun `aprendizado com uma pergunta especifica monta partida de uma pergunta`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))
        questionRepository.save(Fixtures.question(id = "k-2", category = Category.KOTLIN))

        val game = useCase.create(UserId("u-1"), GameMode.APRENDIZADO, null, QuestionId("k-2")).game

        assertEquals(1, game.totalQuestions)
        assertEquals("k-2", game.currentQuestion().idText)
    }

    @Test
    fun `aprendizado com pergunta inexistente falha`() {
        userRepository.save(Fixtures.user(id = "u-1"))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, null, QuestionId("nao-existe"))
        }

        assertEquals("QUESTION_NOT_FOUND", error.code)
    }

    @Test
    fun `aprendizado com pergunta inativa nao inicia`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(
            Fixtures.question(id = "k-1", category = Category.KOTLIN).deactivate(TestClock.fixed.instant())
        )

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, null, QuestionId("k-1"))
        }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `aprendizado sem perguntas no tema nao inicia`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "r-1", category = Category.REST))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, Category.KOTLIN, null)
        }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `modo gubee usa apenas as perguntas da categoria gubee`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "g-1", category = Category.GUBEE))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        val game = useCase.create(UserId("u-1"), GameMode.GUBEE, null, null).game

        assertEquals(1, game.totalQuestions)
        assertEquals(Category.GUBEE, game.currentQuestion().category)
    }

    @Test
    fun `modo gubee sem perguntas nao inicia`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.GUBEE, null, null)
        }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `classico ignora as perguntas gubee`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "g-1", category = Category.GUBEE))
        questionRepository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        val game = useCase.create(UserId("u-1"), GameMode.CLASSIC, null, null).game

        assertEquals(1, game.totalQuestions)
        assertEquals(Category.KOTLIN, game.currentQuestion().category)
    }

    @Test
    fun `aprendizado nao aceita o tema gubee`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "g-1", category = Category.GUBEE))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, Category.GUBEE, null)
        }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }

    @Test
    fun `aprendizado nao aceita pergunta gubee`() {
        userRepository.save(Fixtures.user(id = "u-1"))
        questionRepository.save(Fixtures.question(id = "g-1", category = Category.GUBEE))

        val error = assertThrows<DomainException> {
            useCase.create(UserId("u-1"), GameMode.APRENDIZADO, null, QuestionId("g-1"))
        }

        assertEquals("NO_ACTIVE_QUESTIONS", error.code)
    }
}
