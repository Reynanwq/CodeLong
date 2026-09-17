package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetCurrentUserUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: GetCurrentUserUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        useCase = GetCurrentUserUseCase(repository)
    }

    @Test
    fun `devolve o usuario autenticado`() {
        repository.save(Fixtures.user(id = "u-1", username = "alice"))

        val user = useCase.get(UserId("u-1"))

        assertEquals(UserId("u-1"), user.id)
        assertEquals("alice", user.username.value)
        assertEquals("alice@codelong.dev", user.email.value)
        assertEquals(Role.USER, user.role)
        assertEquals(AccountStatus.ACTIVE, user.status())
    }

    @Test
    fun `devolve o administrador`() {
        repository.save(Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN))

        assertEquals(Role.ADMIN, useCase.get(UserId("a-1")).role)
    }

    @Test
    fun `devolve a instancia armazenada`() {
        val user = Fixtures.user(id = "u-1")
        repository.save(user)

        assertSame(repository.findById(UserId("u-1")), useCase.get(UserId("u-1")))
    }

    @Test
    fun `devolve usuario inativo`() {
        repository.save(Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant()))

        assertEquals(AccountStatus.INACTIVE, useCase.get(UserId("u-1")).status())
    }

    @Test
    fun `usuario inexistente gera erro`() {
        val error = assertThrows<DomainException> { useCase.get(UserId("ninguem")) }

        assertEquals("USER_NOT_FOUND", error.code)
        assertEquals("User not found", error.message)
    }
}
