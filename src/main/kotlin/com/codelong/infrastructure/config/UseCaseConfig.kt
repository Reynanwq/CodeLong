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
import com.codelong.application.usecase.ListUsersUseCase
import com.codelong.application.usecase.LoginUserUseCase
import com.codelong.application.usecase.RegisterUserUseCase
import com.codelong.application.usecase.UpdateQuestionUseCase
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.port.TokenService
import com.codelong.domain.port.UserRepository
import com.codelong.domain.service.GameSequencer
import com.codelong.infrastructure.security.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import kotlin.random.Random

/**
 * Wiring dos servicos e casos de uso. Mantem a camada de aplicacao livre de
 * anotacoes de framework.
 */
@Configuration
class UseCaseConfig {

    @Bean
    fun random(): Random = Random.Default

    @Bean
    fun gameSequencer(random: Random): GameSequencer = GameSequencer(random)

    @Bean
    fun tokenIssuer(
        tokenService: TokenService,
        clock: Clock,
        properties: SecurityProperties
    ): TokenIssuer = TokenIssuer(tokenService, clock, properties.jwt.expiration)

    @Bean
    fun userFactory(passwordEncoder: PasswordEncoder, clock: Clock): UserFactory =
        UserFactory(passwordEncoder, clock)

    @Bean
    fun passwordPolicy(): PasswordPolicy = PasswordPolicy()

    @Bean
    fun gameFactory(gameSequencer: GameSequencer, clock: Clock): GameFactory =
        GameFactory(gameSequencer, clock)

    @Bean
    fun registerUserUseCase(
        userRepository: UserRepository,
        userFactory: UserFactory,
        tokenIssuer: TokenIssuer,
        passwordPolicy: PasswordPolicy
    ) = RegisterUserUseCase(userRepository, userFactory, tokenIssuer, passwordPolicy)

    @Bean
    fun changePasswordUseCase(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        passwordPolicy: PasswordPolicy,
        clock: Clock
    ) = ChangePasswordUseCase(userRepository, passwordEncoder, passwordPolicy, clock)

    @Bean
    fun loginUserUseCase(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        tokenIssuer: TokenIssuer
    ) = LoginUserUseCase(userRepository, passwordEncoder, tokenIssuer)

    @Bean
    fun getCurrentUserUseCase(userRepository: UserRepository) =
        GetCurrentUserUseCase(userRepository)

    @Bean
    fun listUsersUseCase(userRepository: UserRepository) =
        ListUsersUseCase(userRepository)

    @Bean
    fun changeUserStatusUseCase(userRepository: UserRepository, clock: Clock) =
        ChangeUserStatusUseCase(userRepository, clock)

    @Bean
    fun createGameUseCase(
        userRepository: UserRepository,
        questionRepository: QuestionRepository,
        gameRepository: GameRepository,
        gameFactory: GameFactory
    ) = CreateGameUseCase(userRepository, questionRepository, gameRepository, gameFactory)

    @Bean
    fun getGameUseCase(gameRepository: GameRepository) = GetGameUseCase(gameRepository)

    @Bean
    fun listGamesUseCase(gameRepository: GameRepository) = ListGamesUseCase(gameRepository)

    @Bean
    fun getInProgressGameUseCase(gameRepository: GameRepository) =
        GetInProgressGameUseCase(gameRepository)

    @Bean
    fun getCurrentQuestionUseCase(gameRepository: GameRepository) =
        GetCurrentQuestionUseCase(gameRepository)

    @Bean
    fun answerQuestionUseCase(gameRepository: GameRepository, clock: Clock) =
        AnswerQuestionUseCase(gameRepository, clock)

    @Bean
    fun abandonGameUseCase(gameRepository: GameRepository, clock: Clock) =
        AbandonGameUseCase(gameRepository, clock)

    @Bean
    fun getRankingUseCase(rankingRepository: RankingRepository) =
        GetRankingUseCase(rankingRepository)

    @Bean
    fun getMyRankingUseCase(rankingRepository: RankingRepository) =
        GetMyRankingUseCase(rankingRepository)

    @Bean
    fun createQuestionUseCase(questionRepository: QuestionRepository, clock: Clock) =
        CreateQuestionUseCase(questionRepository, clock)

    @Bean
    fun updateQuestionUseCase(questionRepository: QuestionRepository, clock: Clock) =
        UpdateQuestionUseCase(questionRepository, clock)

    @Bean
    fun changeQuestionStatusUseCase(questionRepository: QuestionRepository, clock: Clock) =
        ChangeQuestionStatusUseCase(questionRepository, clock)

    @Bean
    fun getQuestionUseCase(questionRepository: QuestionRepository) =
        GetQuestionUseCase(questionRepository)

    @Bean
    fun listQuestionsUseCase(questionRepository: QuestionRepository) =
        ListQuestionsUseCase(questionRepository)

    @Bean
    fun deleteQuestionUseCase(questionRepository: QuestionRepository) =
        DeleteQuestionUseCase(questionRepository)
}