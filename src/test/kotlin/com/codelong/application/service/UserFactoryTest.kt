package com.codelong.application.service

import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.Username
import com.codelong.support.FakePasswordEncoder
import com.codelong.support.Fixtures
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UserFactoryTest {

    private val factory = UserFactory(FakePasswordEncoder(), TestClock.fixed)

    @Test
    fun `createUser cria usuario com papel USER`() {
        val user = factory.createUser(
            Username.of("alice"),
            Email.of("alice@codelong.dev"),
            "secret123"
        )

        assertEquals(Role.USER, user.role)
        assertEquals("alice", user.username.value)
        assertEquals("alice@codelong.dev", user.email.value)
        assertTrue(user.isActive())
    }

    @Test
    fun `createAdmin cria usuario com papel ADMIN`() {
        val admin = factory.createAdmin(
            Username.of("admin"),
            Email.of("admin@codelong.local"),
            "admin12345"
        )

        assertEquals(Role.ADMIN, admin.role)
        assertTrue(admin.isAdmin())
    }

    @Test
    fun `senha e armazenada apenas como hash`() {
        val user = factory.createUser(Username.of("alice"), Email.of("alice@codelong.dev"), "secret123")

        assertEquals(PasswordHash("hashed:secret123"), user.passwordHash())
    }

    @Test
    fun `usa o instante do clock`() {
        val user = factory.createUser(Username.of("alice"), Email.of("alice@codelong.dev"), "secret123")

        assertEquals(Fixtures.NOW, user.createdAt)
        assertEquals(Fixtures.NOW, user.updatedAt())
    }

    @Test
    fun `gera identificadores distintos`() {
        val ids = (1..200).map {
            factory.createUser(Username.of("user$it"), Email.of("user$it@codelong.dev"), "secret123").id.value
        }

        assertEquals(200, ids.distinct().size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["alice", "bob", "user_name", "a-b", "x.y"])
    fun `preserva o username informado`(username: String) {
        val user = factory.createUser(
            Username.of(username),
            Email.of("$username@codelong.dev"),
            "secret123"
        )

        assertEquals(username, user.username.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["alice", "bob", "carol"])
    fun `preserva o email normalizado`(username: String) {
        val user = factory.createUser(
            Username.of(username),
            Email.of("${username}@CodeLong.DEV"),
            "secret123"
        )

        assertEquals("$username@codelong.dev", user.email.value)
    }

    @Test
    fun `createAdmin nao altera o username nem o email`() {
        val admin = factory.createAdmin(Username.of("root"), Email.of("root@codelong.dev"), "root12345")

        assertEquals("root", admin.username.value)
        assertEquals("root@codelong.dev", admin.email.value)
    }
}
