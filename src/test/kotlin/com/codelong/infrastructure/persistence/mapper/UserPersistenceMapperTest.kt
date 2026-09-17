package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.infrastructure.persistence.document.UserDocument
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserPersistenceMapperTest {

    @Test
    fun `toDocument copia todos os campos do usuario`() {
        val user = Fixtures.user(id = "u-1", username = "alice", role = Role.ADMIN)

        val document = UserPersistenceMapper.toDocument(user)

        assertEquals("u-1", document.id)
        assertEquals("alice", document.username)
        assertEquals("alice@codelong.dev", document.email)
        assertEquals("hashed:secret123", document.passwordHash)
        assertEquals("ADMIN", document.role)
        assertEquals("ACTIVE", document.status)
        assertEquals(Fixtures.NOW, document.createdAt)
        assertEquals(Fixtures.NOW, document.updatedAt)
    }

    @Test
    fun `toDomain reconstroi o usuario`() {
        val document = UserDocument(
            id = "u-1",
            username = "alice",
            email = "alice@codelong.dev",
            passwordHash = "hashed:abc",
            role = "USER",
            status = "ACTIVE",
            createdAt = Fixtures.NOW,
            updatedAt = Fixtures.NOW
        )

        val user = UserPersistenceMapper.toDomain(document)

        assertEquals("u-1", user.id.value)
        assertEquals("alice", user.username.value)
        assertEquals("alice@codelong.dev", user.email.value)
        assertEquals(PasswordHash("hashed:abc"), user.passwordHash())
        assertEquals(Role.USER, user.role)
        assertEquals(AccountStatus.ACTIVE, user.status())
        assertEquals(Fixtures.NOW, user.createdAt)
    }

    @Test
    fun `round trip preserva o estado`() {
        val original = Fixtures.user(id = "u-1", username = "alice")
            .changePassword(PasswordHash("outro-hash"), Fixtures.NOW.plusSeconds(60))
            .deactivate(Fixtures.NOW.plusSeconds(120))

        val restored = UserPersistenceMapper.toDomain(UserPersistenceMapper.toDocument(original))

        assertEquals(original.id, restored.id)
        assertEquals(original.username, restored.username)
        assertEquals(original.email, restored.email)
        assertEquals(original.passwordHash(), restored.passwordHash())
        assertEquals(original.role, restored.role)
        assertEquals(original.status(), restored.status())
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt(), restored.updatedAt())
        assertFalse(restored.isActive())
    }

    @Test
    fun `toDomain aceita papel em caixa baixa`() {
        val document = UserDocument(
            id = "u-1",
            username = "alice",
            email = "alice@codelong.dev",
            passwordHash = "hash",
            role = "admin",
            status = "ACTIVE",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )

        assertEquals(Role.ADMIN, UserPersistenceMapper.toDomain(document).role)
    }

    @Test
    fun `toDomain mapeia usuario inativo`() {
        val document = UserDocument(
            id = "u-1",
            username = "alice",
            email = "alice@codelong.dev",
            passwordHash = "hash",
            role = "USER",
            status = "INACTIVE"
        )

        val user = UserPersistenceMapper.toDomain(document)

        assertEquals(AccountStatus.INACTIVE, user.status())
        assertFalse(user.isActive())
    }

    @Test
    fun `toDocument de administrador preserva o papel`() {
        val admin = Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN)

        assertTrue(UserPersistenceMapper.toDomain(UserPersistenceMapper.toDocument(admin)).isAdmin())
    }

    @Test
    fun `toDomain normaliza o email`() {
        val document = UserDocument(
            id = "u-1",
            username = "alice",
            email = "  ALICE@CodeLong.DEV  ",
            passwordHash = "hash",
            role = "USER",
            status = "ACTIVE"
        )

        assertEquals("alice@codelong.dev", UserPersistenceMapper.toDomain(document).email.value)
    }
}
