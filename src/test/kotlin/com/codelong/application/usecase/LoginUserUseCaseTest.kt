package com.codelong.application.usecase

import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.command.LoginCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.domain.exception.UnauthorizedException
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.FakeTokenService
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class LoginUserUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var registerUseCase: RegisterUserUseCase
    private lateinit var loginUseCase: LoginUserUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        val encoder = FakePasswordEncoder()
        val tokenIssuer = TokenIssuer(FakeTokenService(), TestClock.fixed, Duration.ofHours(8))
        registerUseCase = RegisterUserUseCase(
            repository,
            UserFactory(encoder, TestClock.fixed),
            tokenIssuer,
            PasswordPolicy()
        )
        loginUseCase = LoginUserUseCase(repository, encoder, tokenIssuer)
    }

    @Test
    fun `autentica por username`() {
        registerUseCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        val result = loginUseCase.login(LoginCommand("dev", "secret123"))

        assertEquals("dev", result.user.username.value)
        assertEquals("token-" + result.user.id.value, result.token)
    }

    @Test
    fun `autentica por email`() {
        registerUseCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        val result = loginUseCase.login(LoginCommand("dev@codelong.dev", "secret123"))

        assertEquals("dev", result.user.username.value)
    }

    @Test
    fun `rejeita senha incorreta`() {
        registerUseCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        val error = assertThrows<UnauthorizedException> {
            loginUseCase.login(LoginCommand("dev", "senha-errada"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `rejeita usuario inexistente`() {
        val error = assertThrows<UnauthorizedException> {
            loginUseCase.login(LoginCommand("ninguem", "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `rejeita conta inativa`() {
        registerUseCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))
        val stored = repository.all().single()
        repository.save(stored.deactivate(TestClock.fixed.instant()))

        val error = assertThrows<UnauthorizedException> {
            loginUseCase.login(LoginCommand("dev", "secret123"))
        }

        assertEquals("ACCOUNT_INACTIVE", error.code)
    }
}