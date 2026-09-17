package com.codelong.application.usecase

import com.codelong.application.service.DefaultUserFactory

import com.codelong.application.service.DefaultTokenIssuer

import com.codelong.application.service.DefaultPasswordPolicy

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.application.service.TokenIssuer
import com.codelong.application.service.UserFactory
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.FakeTokenService
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class RegisterUserUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: RegisterUserUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        useCase = RegisterUserUseCaseImpl(
            userRepository = repository,
            userFactory = DefaultUserFactory(FakePasswordEncoder(), TestClock.fixed),
            tokenIssuer = DefaultTokenIssuer(FakeTokenService(), TestClock.fixed, Duration.ofHours(8)),
            passwordPolicy = DefaultPasswordPolicy()
        )
    }

    @Test
    fun `registra usuario com senha hasheada e emite token`() {
        val result = useCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        assertEquals("dev", result.user.username.value)
        assertEquals("hashed:secret123", result.user.passwordHash().value)
        assertTrue(result.user.isActive())
        assertTrue(result.token.startsWith("token-"))
        assertEquals(1, repository.all().size)
    }

    @Test
    fun `rejeita username duplicado`() {
        useCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("dev", "outro@codelong.dev", "secret123"))
        }

        assertEquals("USERNAME_ALREADY_EXISTS", error.code)
    }

    @Test
    fun `rejeita email duplicado`() {
        useCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "secret123"))

        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("dev2", "dev@codelong.dev", "secret123"))
        }

        assertEquals("EMAIL_ALREADY_EXISTS", error.code)
    }

    @Test
    fun `rejeita senha fraca`() {
        val error = assertThrows<DomainException> {
            useCase.register(RegisterUserCommand("dev", "dev@codelong.dev", "curta"))
        }

        assertEquals("password.tooWeak", error.code)
    }
}