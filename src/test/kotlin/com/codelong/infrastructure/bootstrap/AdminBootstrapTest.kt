package com.codelong.infrastructure.bootstrap

import com.codelong.application.service.DefaultUserFactory

import com.codelong.application.service.UserFactory
import com.codelong.domain.valueobject.Role
import com.codelong.infrastructure.security.SecurityProperties
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.InMemoryUserRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class AdminBootstrapTest {

    private lateinit var repository: InMemoryUserRepository
    private lateinit var factory: UserFactory
    private val args = DefaultApplicationArguments()

    @BeforeEach
    fun setUp() {
        repository = InMemoryUserRepository()
        factory = DefaultUserFactory(FakePasswordEncoder(), TestClock.fixed)
    }

    private fun bootstrap(username: String, password: String) = AdminBootstrap(
        repository,
        factory,
        SecurityProperties(admin = SecurityProperties.Admin(username = username, password = password))
    )

    @Test
    fun `nao cria admin quando username esta vazio`() {
        bootstrap("", "admin12345").run(args)

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun `nao cria admin quando password esta vazia`() {
        bootstrap("admin", "").run(args)

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun `nao cria admin quando ambos estao vazios`() {
        bootstrap("", "").run(args)

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun `username apenas com espacos e ignorado`() {
        bootstrap("   ", "admin12345").run(args)

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun `cria o admin quando configurado e inexistente`() {
        bootstrap("admin", "admin12345").run(args)

        val admin = repository.all().single()
        assertEquals("admin", admin.username.value)
        assertEquals(Role.ADMIN, admin.role)
        assertTrue(admin.isAdmin())
        assertTrue(admin.isActive())
    }

    @Test
    fun `admin criado usa o email padrao`() {
        bootstrap("admin", "admin12345").run(args)

        assertEquals("admin@codelong.local", repository.all().single().email.value)
    }

    @Test
    fun `admin criado usa o hash da senha`() {
        bootstrap("admin", "admin12345").run(args)

        assertEquals("hashed:admin12345", repository.all().single().passwordHash().value)
    }

    @Test
    fun `remove espacos do username antes de criar`() {
        bootstrap("  admin  ", "admin12345").run(args)

        assertEquals("admin", repository.all().single().username.value)
        assertEquals("admin@codelong.local", repository.all().single().email.value)
    }

    @Test
    fun `nao cria admin duplicado quando ja existe`() {
        repository.save(factory.createAdmin(com.codelong.domain.valueobject.Username.of("admin"), com.codelong.domain.valueobject.Email.of("admin@codelong.local"), "outra-senha"))

        bootstrap("admin", "admin12345").run(args)

        assertEquals(1, repository.all().size)
        assertEquals("hashed:outra-senha", repository.all().single().passwordHash().value)
    }

    @Test
    fun `pode ser executado mais de uma vez sem efeito colateral`() {
        val bootstrap = bootstrap("admin", "admin12345")

        bootstrap.run(args)
        bootstrap.run(args)

        assertEquals(1, repository.all().size)
    }

    @Test
    fun `nao cria admin quando password esta preenchida mas username nao`() {
        bootstrap("", "admin12345").run(args)
        bootstrap("admin", "admin12345").run(args)

        assertEquals(1, repository.all().size)
    }
}
