package com.codelong.infrastructure.config

import com.codelong.application.service.GameFactory
import com.codelong.application.service.DefaultGameFactory
import com.codelong.application.service.DefaultPasswordPolicy
import com.codelong.application.service.DefaultTokenIssuer
import com.codelong.application.service.DefaultUserFactory
import com.codelong.application.usecase.AbandonGameUseCaseImpl
import com.codelong.application.usecase.AnswerQuestionUseCaseImpl
import com.codelong.application.usecase.ChangePasswordUseCaseImpl
import com.codelong.application.usecase.ChangeQuestionStatusUseCaseImpl
import com.codelong.application.usecase.ChangeUserStatusUseCaseImpl
import com.codelong.application.usecase.CreateGameUseCaseImpl
import com.codelong.application.usecase.CreateQuestionUseCaseImpl
import com.codelong.application.usecase.DeleteQuestionUseCaseImpl
import com.codelong.application.usecase.GetCurrentQuestionUseCaseImpl
import com.codelong.application.usecase.GetCurrentUserUseCaseImpl
import com.codelong.application.usecase.GetGameUseCaseImpl
import com.codelong.application.usecase.GetInProgressGameUseCaseImpl
import com.codelong.application.usecase.GetMyRankingUseCaseImpl
import com.codelong.application.usecase.GetQuestionUseCaseImpl
import com.codelong.application.usecase.GetRankingUseCaseImpl
import com.codelong.application.usecase.ListGamesUseCaseImpl
import com.codelong.application.usecase.ListQuestionsUseCaseImpl
import com.codelong.application.usecase.ListThemeQuestionsUseCaseImpl
import com.codelong.application.usecase.ListThemesUseCaseImpl
import com.codelong.application.usecase.ListUsersUseCaseImpl
import com.codelong.application.usecase.LoginUserUseCaseImpl
import com.codelong.application.usecase.RegisterUserUseCaseImpl
import com.codelong.application.usecase.UpdateQuestionUseCaseImpl
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
    ): TokenIssuer = DefaultTokenIssuer(tokenService, clock, properties.jwt.expiration)

    @Bean
    fun userFactory(passwordEncoder: PasswordEncoder, clock: Clock): UserFactory =
        DefaultUserFactory(passwordEncoder, clock)

    @Bean
    fun passwordPolicy(): PasswordPolicy = DefaultPasswordPolicy()

    @Bean
    fun gameFactory(gameSequencer: GameSequencer, clock: Clock): GameFactory =
        DefaultGameFactory(gameSequencer, clock)

    @Bean
    fun registerUserUseCase(
        userRepository: UserRepository,
        userFactory: UserFactory,
        tokenIssuer: TokenIssuer,
        passwordPolicy: PasswordPolicy
    ) = RegisterUserUseCaseImpl(userRepository, userFactory, tokenIssuer, passwordPolicy)

    @Bean
    fun changePasswordUseCase(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        passwordPolicy: PasswordPolicy,
        clock: Clock
    ) = ChangePasswordUseCaseImpl(userRepository, passwordEncoder, passwordPolicy, clock)

    @Bean
    fun loginUserUseCase(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        tokenIssuer: TokenIssuer
    ) = LoginUserUseCaseImpl(userRepository, passwordEncoder, tokenIssuer)

    @Bean
    fun getCurrentUserUseCase(userRepository: UserRepository) =
        GetCurrentUserUseCaseImpl(userRepository)

    @Bean
    fun listUsersUseCase(userRepository: UserRepository) =
        ListUsersUseCaseImpl(userRepository)

    @Bean
    fun changeUserStatusUseCase(userRepository: UserRepository, clock: Clock) =
        ChangeUserStatusUseCaseImpl(userRepository, clock)

    @Bean
    fun createGameUseCase(
        userRepository: UserRepository,
        questionRepository: QuestionRepository,
        gameRepository: GameRepository,
        gameFactory: GameFactory
    ) = CreateGameUseCaseImpl(userRepository, questionRepository, gameRepository, gameFactory)

    @Bean
    fun getGameUseCase(gameRepository: GameRepository) = GetGameUseCaseImpl(gameRepository)

    @Bean
    fun listGamesUseCase(gameRepository: GameRepository) = ListGamesUseCaseImpl(gameRepository)

    @Bean
    fun getInProgressGameUseCase(gameRepository: GameRepository) =
        GetInProgressGameUseCaseImpl(gameRepository)

    @Bean
    fun getCurrentQuestionUseCase(gameRepository: GameRepository, clock: Clock) =
        GetCurrentQuestionUseCaseImpl(gameRepository, clock)

    @Bean
    fun answerQuestionUseCase(gameRepository: GameRepository, clock: Clock) =
        AnswerQuestionUseCaseImpl(gameRepository, clock)

    @Bean
    fun abandonGameUseCase(gameRepository: GameRepository, clock: Clock) =
        AbandonGameUseCaseImpl(gameRepository, clock)

    @Bean
    fun getRankingUseCase(rankingRepository: RankingRepository) =
        GetRankingUseCaseImpl(rankingRepository)

    @Bean
    fun getMyRankingUseCase(rankingRepository: RankingRepository) =
        GetMyRankingUseCaseImpl(rankingRepository)

    @Bean
    fun createQuestionUseCase(questionRepository: QuestionRepository, clock: Clock) =
        CreateQuestionUseCaseImpl(questionRepository, clock)

    @Bean
    fun updateQuestionUseCase(questionRepository: QuestionRepository, clock: Clock) =
        UpdateQuestionUseCaseImpl(questionRepository, clock)

    @Bean
    fun changeQuestionStatusUseCase(questionRepository: QuestionRepository, clock: Clock) =
        ChangeQuestionStatusUseCaseImpl(questionRepository, clock)

    @Bean
    fun getQuestionUseCase(questionRepository: QuestionRepository) =
        GetQuestionUseCaseImpl(questionRepository)

    @Bean
    fun listQuestionsUseCase(questionRepository: QuestionRepository) =
        ListQuestionsUseCaseImpl(questionRepository)

    @Bean
    fun deleteQuestionUseCase(questionRepository: QuestionRepository) =
        DeleteQuestionUseCaseImpl(questionRepository)

    @Bean
    fun listThemesUseCase(questionRepository: QuestionRepository) =
        ListThemesUseCaseImpl(questionRepository)

    @Bean
    fun listThemeQuestionsUseCase(questionRepository: QuestionRepository) =
        ListThemeQuestionsUseCaseImpl(questionRepository)
}