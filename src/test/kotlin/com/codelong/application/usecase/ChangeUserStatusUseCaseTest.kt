package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangeUserStatusCommand
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChangeUserStatusUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: ChangeUserStatusUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        useCase = ChangeUserStatusUseCaseImpl(repository, TestClock.fixed)
    }

    @Test
    fun `admin desativa outro usuario`() {
        repository.save(Fixtures.user(id = "u-1", username = "alice"))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("u-1"), active = false),
            actorId = UserId("admin-1")
        )

        assertEquals(AccountStatus.INACTIVE, user.status())
        assertFalse(user.isActive())
    }

    @Test
    fun `admin ativa outro usuario`() {
        repository.save(Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant()))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("u-1"), active = true),
            actorId = UserId("admin-1")
        )

        assertEquals(AccountStatus.ACTIVE, user.status())
        assertTrue(user.isActive())
    }

    @Test
    fun `atualiza a data de modificacao`() {
        repository.save(Fixtures.user(id = "u-1"))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("u-1"), active = false),
            actorId = UserId("admin-1")
        )

        assertEquals(Fixtures.NOW, user.updatedAt())
        assertEquals(Fixtures.NOW, user.createdAt)
    }

    @Test
    fun `persiste a mudanca`() {
        repository.save(Fixtures.user(id = "u-1"))

        useCase.change(ChangeUserStatusCommand(UserId("u-1"), active = false), actorId = UserId("admin-1"))

        assertEquals(AccountStatus.INACTIVE, repository.findById(UserId("u-1"))!!.status())
    }

    @Test
    fun `admin nao pode desativar a propria conta`() {
        repository.save(Fixtures.user(id = "admin-1", username = "admin", role = Role.ADMIN))

        val error = assertThrows<DomainException> {
            useCase.change(ChangeUserStatusCommand(UserId("admin-1"), active = false), actorId = UserId("admin-1"))
        }

        assertEquals("user.deactivate.self", error.code)
        assertEquals("An administrator cannot deactivate their own account", error.message)
    }

    @Test
    fun `admin pode ativar a propria conta`() {
        repository.save(Fixtures.user(id = "admin-1", username = "admin", role = Role.ADMIN))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("admin-1"), active = true),
            actorId = UserId("admin-1")
        )

        assertEquals(AccountStatus.ACTIVE, user.status())
    }

    @Test
    fun `usuario inexistente gera erro`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangeUserStatusCommand(UserId("nao-existe"), active = false), actorId = UserId("admin-1"))
        }

        assertEquals("USER_NOT_FOUND", error.code)
    }

    @Test
    fun `nao altera outros usuarios`() {
        repository.save(Fixtures.user(id = "u-1"))
        repository.save(Fixtures.user(id = "u-2"))

        useCase.change(ChangeUserStatusCommand(UserId("u-1"), active = false), actorId = UserId("admin-1"))

        assertEquals(AccountStatus.ACTIVE, repository.findById(UserId("u-2"))!!.status())
    }

    @Test
    fun `desativar usuario ja inativo e permitido`() {
        repository.save(Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant()))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("u-1"), active = false),
            actorId = UserId("admin-1")
        )

        assertEquals(AccountStatus.INACTIVE, user.status())
    }

    @Test
    fun `devolve a mesma instancia do repositorio`() {
        repository.save(Fixtures.user(id = "u-1"))

        val user = useCase.change(
            ChangeUserStatusCommand(UserId("u-1"), active = false),
            actorId = UserId("admin-1")
        )

        assertSame(repository.findById(UserId("u-1")), user)
    }
}
