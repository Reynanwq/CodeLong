package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangeUserStatusCommand
import com.codelong.application.command.UserSearchQuery
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserManagementUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var listUseCase: ListUsersUseCase
    private lateinit var changeStatusUseCase: ChangeUserStatusUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        listUseCase = ListUsersUseCase(repository)
        changeStatusUseCase = ChangeUserStatusUseCase(repository, TestClock.fixed)

        repository.save(Fixtures.user(id = "admin-1", username = "admin", role = Role.ADMIN))
        repository.save(Fixtures.user(id = "user-1", username = "alice"))
        repository.save(Fixtures.user(id = "user-2", username = "bob"))
    }

    @Test
    fun `lista usuarios de forma paginada`() {
        val firstPage = listUseCase.list(UserSearchQuery(page = 0, size = 2))
        assertEquals(3L, firstPage.totalElements)
        assertEquals(listOf("admin", "alice"), firstPage.items.map { it.username.value })

        val secondPage = listUseCase.list(UserSearchQuery(page = 1, size = 2))
        assertEquals(listOf("bob"), secondPage.items.map { it.username.value })
    }

    @Test
    fun `filtra por status e papel`() {
        changeStatusUseCase.change(ChangeUserStatusCommand(UserId("user-2"), false), UserId("admin-1"))

        val inactive = listUseCase.list(UserSearchQuery(status = AccountStatus.INACTIVE))
        assertEquals(listOf("bob"), inactive.items.map { it.username.value })

        val admins = listUseCase.list(UserSearchQuery(role = Role.ADMIN))
        assertEquals(listOf("admin"), admins.items.map { it.username.value })
    }

    @Test
    fun `desativa e reativa uma conta`() {
        val deactivated = changeStatusUseCase.change(
            ChangeUserStatusCommand(UserId("user-1"), false),
            UserId("admin-1")
        )
        assertEquals(AccountStatus.INACTIVE, deactivated.status())

        val reactivated = changeStatusUseCase.change(
            ChangeUserStatusCommand(UserId("user-1"), true),
            UserId("admin-1")
        )
        assertEquals(AccountStatus.ACTIVE, reactivated.status())
    }

    @Test
    fun `administrador nao desativa a propria conta`() {
        val error = assertThrows<DomainException> {
            changeStatusUseCase.change(
                ChangeUserStatusCommand(UserId("admin-1"), false),
                UserId("admin-1")
            )
        }

        assertEquals("user.deactivate.self", error.code)
        assertEquals(AccountStatus.ACTIVE, repository.findById(UserId("admin-1"))?.status())
    }

    @Test
    fun `usuario inexistente`() {
        val error = assertThrows<DomainException> {
            changeStatusUseCase.change(
                ChangeUserStatusCommand(UserId("nao-existe"), false),
                UserId("admin-1")
            )
        }

        assertEquals("USER_NOT_FOUND", error.code)
    }
}
