package com.codelong.infrastructure.config

import com.codelong.application.service.GameFactory
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.application.usecase.AbandonGameUseCase
import com.codelong.application.usecase.AnswerQuestionUseCase
import com.codelong.application.usecase.ChangePasswordUseCase
import com.codelong.application.usecase.ChangeQuestionStatusUseCase
import com.codelong.application.usecase.ChangeUserStatusUseCase
import com.codelong.application.usecase.CreateGameUseCase
import com.codelong.application.usecase.CreateQuestionUseCase
import com.codelong.application.usecase.DeleteQuestionUseCase
import com.codelong.application.usecase.GetCurrentQuestionUseCase
import com.codelong.application.usecase.GetCurrentUserUseCase
import com.codelong.application.usecase.GetGameUseCase
import com.codelong.application.usecase.GetInProgressGameUseCase
import com.codelong.application.usecase.GetMyRankingUseCase
import com.codelong.application.usecase.GetQuestionUseCase
import com.codelong.application.usecase.GetRankingUseCase
import com.codelong.application.usecase.ListGamesUseCase
import com.codelong.application.usecase.ListQuestionsUseCase
import com.codelong.application.usecase.ListThemeQuestionsUseCase
import com.codelong.application.usecase.ListThemesUseCase
import com.codelong.application.usecase.ListUsersUseCase
import com.codelong.application.usecase.LoginUserUseCase
import com.codelong.application.usecase.RegisterUserUseCase
import com.codelong.application.usecase.UpdateQuestionUseCase
import com.codelong.domain.service.GameSequencer
import com.codelong.infrastructure.security.SecurityProperties
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.FakeTokenService
import com.codelong.support.InMemoryGameRepository
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.InMemoryRankingRepository
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset

class ConfigBeansTest {

    private val users = InMemoryUserRepository()
    private val questions = InMemoryQuestionRepository()
    private val games = InMemoryGameRepository()
    private val rankings = InMemoryRankingRepository()
    private val encoder = FakePasswordEncoder()
    private val properties = SecurityProperties(jwt = SecurityProperties.Jwt(secret = "x".repeat(40)))

    private val config = UseCaseConfig()

    @Test
    fun `clock do sistema usa UTC`() {
        val clock = ClockConfig().clock()

        assertEquals(ZoneOffset.UTC, clock.zone)
        assertNotNull(clock.instant())
    }

    @Test
    fun `openapi descreve a API e o esquema bearer`() {
        val openApi = OpenApiConfig().codelongOpenApi()

        assertEquals("CodeLong API", openApi.info.title)
        assertEquals("v1", openApi.info.version)
        assertTrue(openApi.info.description.isNotBlank())
        assertTrue(openApi.components.securitySchemes.containsKey("bearerAuth"))
        assertEquals(1, openApi.security.size)
    }

    @Test
    fun `random e gameSequencer sao criados`() {
        val random = config.random()

        assertNotNull(random)
        assertInstanceOf(GameSequencer::class.java, config.gameSequencer(random))
    }

    @Test
    fun `tokenIssuer usa a expiracao das propriedades`() {
        val issuer = config.tokenIssuer(FakeTokenService(), TestClock.fixed, properties)

        assertInstanceOf(TokenIssuer::class.java, issuer)
    }

    @Test
    fun `userFactory e passwordPolicy sao criados`() {
        assertInstanceOf(UserFactory::class.java, config.userFactory(encoder, TestClock.fixed))
        assertEquals(PasswordPolicy.MIN_LENGTH, 8)
        assertInstanceOf(PasswordPolicy::class.java, config.passwordPolicy())
    }

    @Test
    fun `gameFactory e criado`() {
        assertInstanceOf(GameFactory::class.java, config.gameFactory(config.gameSequencer(config.random()), TestClock.fixed))
    }

    @Test
    fun `beans de usuario sao criados`() {
        val tokenIssuer = config.tokenIssuer(FakeTokenService(), TestClock.fixed, properties)

        assertInstanceOf(
            RegisterUserUseCase::class.java,
            config.registerUserUseCase(users, config.userFactory(encoder, TestClock.fixed), tokenIssuer, config.passwordPolicy())
        )
        assertInstanceOf(
            ChangePasswordUseCase::class.java,
            config.changePasswordUseCase(users, encoder, config.passwordPolicy(), TestClock.fixed)
        )
        assertInstanceOf(LoginUserUseCase::class.java, config.loginUserUseCase(users, encoder, tokenIssuer))
        assertInstanceOf(GetCurrentUserUseCase::class.java, config.getCurrentUserUseCase(users))
        assertInstanceOf(ListUsersUseCase::class.java, config.listUsersUseCase(users))
        assertInstanceOf(ChangeUserStatusUseCase::class.java, config.changeUserStatusUseCase(users, TestClock.fixed))
    }

    @Test
    fun `beans de partida sao criados`() {
        val gameFactory = config.gameFactory(config.gameSequencer(config.random()), TestClock.fixed)

        assertInstanceOf(
            CreateGameUseCase::class.java,
            config.createGameUseCase(users, questions, games, gameFactory)
        )
        assertInstanceOf(GetGameUseCase::class.java, config.getGameUseCase(games))
        assertInstanceOf(ListGamesUseCase::class.java, config.listGamesUseCase(games))
        assertInstanceOf(GetInProgressGameUseCase::class.java, config.getInProgressGameUseCase(games))
        assertInstanceOf(GetCurrentQuestionUseCase::class.java, config.getCurrentQuestionUseCase(games, TestClock.fixed))
        assertInstanceOf(AnswerQuestionUseCase::class.java, config.answerQuestionUseCase(games, TestClock.fixed))
        assertInstanceOf(AbandonGameUseCase::class.java, config.abandonGameUseCase(games, TestClock.fixed))
    }

    @Test
    fun `beans de ranking sao criados`() {
        assertInstanceOf(GetRankingUseCase::class.java, config.getRankingUseCase(rankings))
        assertInstanceOf(GetMyRankingUseCase::class.java, config.getMyRankingUseCase(rankings))
    }

    @Test
    fun `beans de pergunta sao criados`() {
        assertInstanceOf(CreateQuestionUseCase::class.java, config.createQuestionUseCase(questions, TestClock.fixed))
        assertInstanceOf(UpdateQuestionUseCase::class.java, config.updateQuestionUseCase(questions, TestClock.fixed))
        assertInstanceOf(
            ChangeQuestionStatusUseCase::class.java,
            config.changeQuestionStatusUseCase(questions, TestClock.fixed)
        )
        assertInstanceOf(GetQuestionUseCase::class.java, config.getQuestionUseCase(questions))
        assertInstanceOf(ListQuestionsUseCase::class.java, config.listQuestionsUseCase(questions))
        assertInstanceOf(DeleteQuestionUseCase::class.java, config.deleteQuestionUseCase(questions))
        assertInstanceOf(ListThemesUseCase::class.java, config.listThemesUseCase(questions))
        assertInstanceOf(ListThemeQuestionsUseCase::class.java, config.listThemeQuestionsUseCase(questions))
    }

    @Test
    fun `clock injetado no tokenIssuer define a expiracao`() {
        val issuer = config.tokenIssuer(FakeTokenService(), TestClock.fixed, properties)
        val token = issuer.issue(com.codelong.support.Fixtures.user())

        assertEquals("token-u-1", token)
    }

    @Test
    fun `expiracao padrao das propriedades e de oito horas`() {
        assertEquals(Duration.ofHours(8), SecurityProperties().jwt.expiration)
    }

    @Test
    fun `clock de teste e fixo`() {
        val clock: Clock = TestClock.fixed

        assertEquals(clock.instant(), clock.instant())
    }
}
