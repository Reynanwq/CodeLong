package com.codelong.domain.model

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTest {

    private val now: Instant = Fixtures.NOW
    private val later: Instant = now.plusSeconds(3600)

    @Test
    fun `create inicia o usuario ativo com datas iguais`() {
        val user = Fixtures.user(id = "u-1", username = "alice")

        assertEquals(UserId("u-1"), user.id)
        assertEquals("alice", user.username.value)
        assertEquals("alice@codelong.dev", user.email.value)
        assertEquals(Role.USER, user.role)
        assertEquals(AccountStatus.ACTIVE, user.status)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.updatedAt)
        assertTrue(user.isActive)
    }

    @Test
    fun `create com papel ADMIN resulta em isAdmin`() {
        val admin = Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN)

        assertTrue(admin.isAdmin)
        assertEquals(Role.ADMIN, admin.role)
    }

    @Test
    fun `usuario comum nao e admin`() {
        assertFalse(Fixtures.user().isAdmin)
    }

    @Test
    fun `passwordHash retorna o hash do perfil`() {
        val user = Fixtures.user()

        assertEquals(PasswordHash("hashed:secret123"), user.passwordHash)
    }

    @Test
    fun `changePassword troca o hash e atualiza a data`() {
        val user = Fixtures.user()

        val returned = user.changePassword(PasswordHash("novo-hash"), later)

        assertSame(user, returned)
        assertEquals(PasswordHash("novo-hash"), user.passwordHash)
        assertEquals(later, user.updatedAt)
        assertEquals(now, user.createdAt)
    }

    @Test
    fun `deactivate muda o status para INACTIVE`() {
        val user = Fixtures.user()

        val returned = user.deactivate(later)

        assertSame(user, returned)
        assertEquals(AccountStatus.INACTIVE, user.status)
        assertFalse(user.isActive)
        assertEquals(later, user.updatedAt)
    }

    @Test
    fun `activate volta o status para ACTIVE`() {
        val user = Fixtures.user().deactivate(now)

        val returned = user.activate(later)

        assertSame(user, returned)
        assertEquals(AccountStatus.ACTIVE, user.status)
        assertTrue(user.isActive)
        assertEquals(later, user.updatedAt)
    }

    @Test
    fun `desativar duas vezes e idempotente`() {
        val user = Fixtures.user().deactivate(later)

        user.deactivate(later.plusSeconds(60))

        assertEquals(AccountStatus.INACTIVE, user.status)
        assertEquals(later.plusSeconds(60), user.updatedAt)
    }

    @Test
    fun `matchesIdentity reconhece o username ignorando a caixa`() {
        val user = Fixtures.user(username = "alice")

        assertTrue(user.matchesIdentity("alice"))
        assertTrue(user.matchesIdentity("ALICE"))
        assertTrue(user.matchesIdentity("AlIcE"))
    }

    @Test
    fun `matchesIdentity reconhece o email ignorando a caixa`() {
        val user = Fixtures.user(username = "alice")

        assertTrue(user.matchesIdentity("alice@codelong.dev"))
        assertTrue(user.matchesIdentity("ALICE@CODELONG.DEV"))
    }

    @Test
    fun `matchesIdentity rejeita identificadores desconhecidos`() {
        val user = Fixtures.user(username = "alice")

        assertFalse(user.matchesIdentity("bob"))
        assertFalse(user.matchesIdentity("bob@codelong.dev"))
        assertFalse(user.matchesIdentity(""))
        assertFalse(user.matchesIdentity(" alice"))
        assertFalse(user.matchesIdentity("alice "))
    }

    @Test
    fun `state reflete o estado atual do usuario`() {
        val user = Fixtures.user(id = "u-1", username = "alice", role = Role.ADMIN)

        val state = user.state()

        assertEquals(UserId("u-1"), state.id)
        assertEquals(Role.ADMIN, state.profile.role)
        assertEquals(AccountStatus.ACTIVE, state.status)
        assertEquals(now, state.createdAt)
        assertEquals(now, state.updatedAt)
    }

    @Test
    fun `reconstitute preserva status e datas`() {
        val original = Fixtures.user(id = "u-1", username = "alice")
            .changePassword(PasswordHash("hash-novo"), later)
            .deactivate(later)

        val restored = User.reconstitute(original.state())

        assertEquals(original.id, restored.id)
        assertEquals(original.username, restored.username)
        assertEquals(original.email, restored.email)
        assertEquals(original.role, restored.role)
        assertEquals(PasswordHash("hash-novo"), restored.passwordHash)
        assertEquals(AccountStatus.INACTIVE, restored.status)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
        assertFalse(restored.isActive)
    }

    @Test
    fun `alteracoes no usuario alteram o state`() {
        val user = Fixtures.user()

        val before = user.state()
        user.deactivate(later)
        val after = user.state()

        assertEquals(AccountStatus.ACTIVE, before.status)
        assertEquals(AccountStatus.INACTIVE, after.status)
        assertFalse(before == after)
    }
}
