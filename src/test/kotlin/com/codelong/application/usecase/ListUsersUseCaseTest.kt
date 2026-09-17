package com.codelong.application.usecase

import com.codelong.application.command.UserSearchQuery
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListUsersUseCaseTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var useCase: ListUsersUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        useCase = ListUsersUseCase(repository)

        repository.save(Fixtures.user(id = "u-1", username = "alice"))
        repository.save(Fixtures.user(id = "u-2", username = "bob"))
        repository.save(Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN))
        repository.save(Fixtures.user(id = "u-3", username = "carol").deactivate(Fixtures.NOW))
    }

    @Test
    fun `sem filtros devolve todos os usuarios`() {
        val page = useCase.list(UserSearchQuery())

        assertEquals(4L, page.totalElements)
        assertEquals(4, page.items.size)
    }

    @Test
    fun `filtra por status`() {
        val active = useCase.list(UserSearchQuery(status = AccountStatus.ACTIVE))
        val inactive = useCase.list(UserSearchQuery(status = AccountStatus.INACTIVE))

        assertEquals(3L, active.totalElements)
        assertEquals(1L, inactive.totalElements)
        assertEquals("carol", inactive.items.first().username.value)
    }

    @Test
    fun `filtra por papel`() {
        val admins = useCase.list(UserSearchQuery(role = Role.ADMIN))
        val users = useCase.list(UserSearchQuery(role = Role.USER))

        assertEquals(1L, admins.totalElements)
        assertEquals(3L, users.totalElements)
        assertEquals("admin", admins.items.first().username.value)
    }

    @Test
    fun `combina filtros de status e papel`() {
        val page = useCase.list(UserSearchQuery(status = AccountStatus.ACTIVE, role = Role.USER))

        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `ordena por username`() {
        val page = useCase.list(UserSearchQuery())

        assertEquals(listOf("admin", "alice", "bob", "carol"), page.items.map { it.username.value })
    }

    @Test
    fun `pagina os resultados`() {
        val first = useCase.list(UserSearchQuery(page = 0, size = 2))
        val second = useCase.list(UserSearchQuery(page = 1, size = 2))

        assertEquals(listOf("admin", "alice"), first.items.map { it.username.value })
        assertEquals(listOf("bob", "carol"), second.items.map { it.username.value })
        assertEquals(4L, first.totalElements)
    }

    @Test
    fun `filtro sem resultados devolve pagina vazia`() {
        val page = useCase.list(UserSearchQuery(role = Role.ADMIN, status = AccountStatus.INACTIVE))

        assertEquals(0L, page.totalElements)
        assertEquals(0, page.items.size)
    }

    @Test
    fun `preserva pagina e tamanho na resposta`() {
        val page = useCase.list(UserSearchQuery(page = 3, size = 15))

        assertEquals(3, page.page)
        assertEquals(15, page.size)
    }
}
