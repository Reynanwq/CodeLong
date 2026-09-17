package com.codelong.application.usecase

import com.codelong.application.service.DefaultUserFactory

import com.codelong.application.service.DefaultTokenIssuer

import com.codelong.application.service.DefaultPasswordPolicy

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.LoginCommand
import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.FakeTokenService
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class LoginUserIdentifierTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var registerUseCase: RegisterUserUseCase
    private lateinit var loginUseCase: LoginUserUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        val encoder = FakePasswordEncoder()
        val tokenIssuer = DefaultTokenIssuer(FakeTokenService(), TestClock.fixed, Duration.ofHours(8))
        registerUseCase = RegisterUserUseCaseImpl(
            repository,
            DefaultUserFactory(encoder, TestClock.fixed),
            tokenIssuer,
            DefaultPasswordPolicy()
        )
        loginUseCase = LoginUserUseCaseImpl(repository, encoder, tokenIssuer)
        registerUseCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))
    }

    @Test
    fun `email em caixa alta e normalizado e aceito`() {
        val result = loginUseCase.login(LoginCommand("DEV@CODELONG.DEV", "secret123"))

        assertEquals("dev", result.user.username.value)
    }

    @Test
    fun `email com espacos nas extremidades e aceito`() {
        val result = loginUseCase.login(LoginCommand("  dev@codelong.dev  ", "secret123"))

        assertEquals("dev", result.user.username.value)
    }

    @Test
    fun `username em caixa alta nao encontra o usuario`() {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand("DEV", "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "ab",
            "usuario@",
            "usuario@invalido",
            "usuario@@exemplo.com",
            "usuário",
            "user#name",
            "user name",
            "sem-arroba"
        ]
    )
    fun `identificador invalido resulta em credenciais invalidas`(identifier: String) {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand(identifier, "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `email inexistente resulta em credenciais invalidas`() {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand("outro@codelong.dev", "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `username valido inexistente resulta em credenciais invalidas`() {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand("outro-dev", "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `senha vazia nao autentica`() {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand("dev", ""))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `identificador de email invalido nao consulta o repositorio`() {
        val error = assertThrows<DomainException> {
            loginUseCase.login(LoginCommand("invalido@", "secret123"))
        }

        assertEquals("INVALID_CREDENTIALS", error.code)
        assertEquals(1, repository.all().size)
    }

    @Test
    fun `login bem sucedido devolve token nao vazio`() {
        val result = loginUseCase.login(LoginCommand("dev", "secret123"))

        assertEquals("token-${result.user.id.value}", result.token)
        assertEquals(true, result.token.isNotBlank())
    }
}
