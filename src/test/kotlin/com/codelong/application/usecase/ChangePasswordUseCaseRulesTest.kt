package com.codelong.application.usecase

import com.codelong.application.command.ChangePasswordCommand
import com.codelong.application.service.PasswordPolicy
import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.exception.UnauthorizedException
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.UserId
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ChangePasswordUseCaseRulesTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var encoder: FakePasswordEncoder
    private lateinit var useCase: ChangePasswordUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        encoder = FakePasswordEncoder()
        useCase = ChangePasswordUseCase(repository, encoder, PasswordPolicy(), TestClock.fixed)
        repository.save(Fixtures.user(id = "u-1", username = "alice"))
    }

    @Test
    fun `troca a senha com sucesso`() {
        val user = useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals(PasswordHash("hashed:novasenha123"), user.passwordHash())
    }

    @Test
    fun `a senha antiga deixa de funcionar`() {
        useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals(false, encoder.matches("secret123", repository.findById(UserId("u-1"))!!.passwordHash()))
        assertEquals(true, encoder.matches("novasenha123", repository.findById(UserId("u-1"))!!.passwordHash()))
    }

    @Test
    fun `atualiza a data de modificacao`() {
        val user = useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals(Fixtures.NOW, user.updatedAt())
        assertEquals(Fixtures.NOW, user.createdAt)
    }

    @Test
    fun `persiste o novo hash`() {
        useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals(PasswordHash("hashed:novasenha123"), repository.findById(UserId("u-1"))!!.passwordHash())
    }

    @Test
    fun `usuario inexistente gera erro`() {
        val error = assertThrows<NotFoundException> {
            useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("ninguem"))
        }

        assertEquals("USER_NOT_FOUND", error.code)
    }

    @Test
    fun `senha atual incorreta gera erro`() {
        val error = assertThrows<UnauthorizedException> {
            useCase.change(ChangePasswordCommand("errada123", "novasenha123"), UserId("u-1"))
        }

        assertEquals("INVALID_CURRENT_PASSWORD", error.code)
        assertEquals("The current password is incorrect", error.message)
    }

    @Test
    fun `senha atual incorreta nao altera o hash`() {
        assertThrows<UnauthorizedException> {
            useCase.change(ChangePasswordCommand("errada123", "novasenha123"), UserId("u-1"))
        }

        assertEquals(PasswordHash("hashed:secret123"), repository.findById(UserId("u-1"))!!.passwordHash())
    }

    @Test
    fun `nova senha igual a atual e recusada`() {
        val error = assertThrows<InvalidInputException> {
            useCase.change(ChangePasswordCommand("secret123", "secret123"), UserId("u-1"))
        }

        assertEquals("password.unchanged", error.code)
        assertEquals("The new password must differ from the current one", error.message)
    }

    @Test
    fun `nova senha curta e recusada`() {
        val error = assertThrows<InvalidInputException> {
            useCase.change(ChangePasswordCommand("secret123", "curta"), UserId("u-1"))
        }

        assertEquals("password.tooWeak", error.code)
    }

    @ParameterizedTest
    @ValueSource(ints = [73, 100])
    fun `nova senha longa demais e recusada`(length: Int) {
        val error = assertThrows<InvalidInputException> {
            useCase.change(ChangePasswordCommand("secret123", "x".repeat(length)), UserId("u-1"))
        }

        assertEquals("password.tooWeak", error.code)
    }

    @Test
    fun `senha atual vazia e recusada`() {
        val error = assertThrows<UnauthorizedException> {
            useCase.change(ChangePasswordCommand("", "novasenha123"), UserId("u-1"))
        }

        assertEquals("INVALID_CURRENT_PASSWORD", error.code)
    }

    @Test
    fun `usuario inativo ainda pode trocar a senha`() {
        repository.save(repository.findById(UserId("u-1"))!!.deactivate(Fixtures.NOW))

        val user = useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals(PasswordHash("hashed:novasenha123"), user.passwordHash())
    }

    @Test
    fun `trocar duas vezes em sequencia funciona`() {
        useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))
        useCase.change(ChangePasswordCommand("novasenha123", "terceirasenha123"), UserId("u-1"))

        assertEquals(PasswordHash("hashed:terceirasenha123"), repository.findById(UserId("u-1"))!!.passwordHash())
    }

    @Test
    fun `nao altera o username nem o papel`() {
        val user = useCase.change(ChangePasswordCommand("secret123", "novasenha123"), UserId("u-1"))

        assertEquals("alice", user.username.value)
        assertEquals(Fixtures.user().role, user.role)
        assertNotEquals(PasswordHash("hashed:secret123"), user.passwordHash())
    }
}
