package com.codelong.application.usecase

import com.codelong.application.service.DefaultUserFactory

import com.codelong.application.service.DefaultTokenIssuer

import com.codelong.application.service.DefaultPasswordPolicy

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.domain.valueobject.Role
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.FakeTokenService
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class RegisterUserUseCaseValidationTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: RegisterUserUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        val encoder = FakePasswordEncoder()
        useCase = RegisterUserUseCaseImpl(
            repository,
            DefaultUserFactory(encoder, TestClock.fixed),
            DefaultTokenIssuer(FakeTokenService(), TestClock.fixed, Duration.ofHours(8)),
            DefaultPasswordPolicy()
        )
    }

    @Test
    fun `cadastra usuario e devolve token`() {
        val result = useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        assertEquals("alice", result.user.username.value)
        assertEquals(Role.USER, result.user.role)
        assertEquals("token-${result.user.id.value}", result.token)
    }

    @Test
    fun `persiste o usuario cadastrado`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        assertEquals(1, repository.all().size)
        assertEquals("alice", repository.all().single().username.value)
    }

    @Test
    fun `armazena apenas o hash da senha`() {
        val result = useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        assertEquals("hashed:secret123", result.user.passwordHash.value)
        assertFalse(result.user.passwordHash.value == "secret123")
    }

    @Test
    fun `normaliza o email`() {
        val result = useCase.register(RegisterUserCommand("alice", "  ALICE@CodeLong.DEV  ", "secret123"))

        assertEquals("alice@codelong.dev", result.user.email.value)
    }

    @Test
    fun `remove espacos do username`() {
        val result = useCase.register(RegisterUserCommand("  alice  ", "alice@codelong.dev", "secret123"))

        assertEquals("alice", result.user.username.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["secret12", "secret123", "uma-senha-bem-longa", "12345678", "Senha_Especial.1"])
    fun `aceita senhas dentro do limite minimo e maximo`(password: String) {
        useCase.register(RegisterUserCommand(password, "$password@codelong.dev", password))

        assertEquals(1, repository.all().size)
    }

    @Test
    fun `aceita senha com exatamente setenta e dois caracteres`() {
        val password = "x".repeat(72)

        useCase.register(RegisterUserCommand("usuario-longo", "longo@codelong.dev", password))

        assertEquals(1, repository.all().size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 5, 7])
    fun `rejeita senha curta demais`(length: Int) {
        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "x".repeat(length)))
        }

        assertEquals("password.tooWeak", error.code)
        assertTrue(repository.all().isEmpty())
    }

    @ParameterizedTest
    @ValueSource(ints = [73, 80, 100])
    fun `rejeita senha longa demais`(length: Int) {
        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "x".repeat(length)))
        }

        assertEquals("password.tooWeak", error.code)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "ab", "usuário", "com espaço", "user@name", "abcdefghijklmnopqrstu"])
    fun `rejeita username invalido`(username: String) {
        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand(username, "alice@codelong.dev", "secret123"))
        }

        assertEquals("username.invalid", error.code)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "sem-arroba", "@example.com", "user@", "user@example", "user@@example.com"])
    fun `rejeita email invalido`(email: String) {
        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("alice", email, "secret123"))
        }

        assertEquals("email.invalid", error.code)
    }

    @Test
    fun `rejeita username duplicado`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("alice", "outro@codelong.dev", "secret123"))
        }

        assertEquals("USERNAME_ALREADY_EXISTS", error.code)
        assertEquals("This username is already taken", error.message)
    }

    @Test
    fun `rejeita email duplicado`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("outro", "alice@codelong.dev", "secret123"))
        }

        assertEquals("EMAIL_ALREADY_EXISTS", error.code)
    }

    @Test
    fun `detecta duplicidade de email ignorando a caixa`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("outro", "ALICE@CODELONG.DEV", "secret123"))
        }
    }

    @Test
    fun `valida a senha antes de checar duplicidade`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "curta"))
        }

        assertEquals("password.tooWeak", error.code)
    }

    @Test
    fun `cadastra usuarios distintos`() {
        useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))
        useCase.register(RegisterUserCommand("bob", "bob@codelong.dev", "secret123"))

        assertEquals(2, repository.all().size)
    }

    @Test
    fun `nao cadastra administrador pelo fluxo publico`() {
        val result = useCase.register(RegisterUserCommand("alice", "alice@codelong.dev", "secret123"))

        assertFalse(result.user.isAdmin)
    }
}
