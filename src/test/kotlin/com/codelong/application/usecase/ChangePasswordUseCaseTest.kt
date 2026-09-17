package com.codelong.application.usecase

import com.codelong.application.service.DefaultPasswordPolicy

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangePasswordCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.domain.valueobject.UserId
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChangePasswordUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: ChangePasswordUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        repository.save(Fixtures.user(id = "user-1", username = "alice"))
        useCase = ChangePasswordUseCaseImpl(
            userRepository = repository,
            passwordEncoder = FakePasswordEncoder(),
            passwordPolicy = DefaultPasswordPolicy(),
            clock = TestClock.fixed
        )
    }

    @Test
    fun `troca a senha quando a senha atual confere`() {
        val updated = useCase.change(
            ChangePasswordCommand("secret123", "novasenha123"),
            UserId("user-1")
        )

        assertEquals("hashed:novasenha123", updated.passwordHash.value)
    }

    @Test
    fun `rejeita senha atual incorreta`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangePasswordCommand("errada123", "novasenha123"), UserId("user-1"))
        }

        assertEquals("INVALID_CURRENT_PASSWORD", error.code)
    }

    @Test
    fun `rejeita nova senha fraca`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangePasswordCommand("secret123", "curta"), UserId("user-1"))
        }

        assertEquals("password.tooWeak", error.code)
    }

    @Test
    fun `rejeita repetir a senha atual`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangePasswordCommand("secret123", "secret123"), UserId("user-1"))
        }

        assertEquals("password.unchanged", error.code)
    }

    @Test
    fun `usuario inexistente`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("nao-existe"))
        }

        assertEquals("USER_NOT_FOUND", error.code)
    }
}
