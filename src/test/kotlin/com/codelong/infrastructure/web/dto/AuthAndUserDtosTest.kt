package com.codelong.infrastructure.web.dto

import com.codelong.application.result.AuthenticationResult
import com.codelong.domain.valueobject.Role
import com.codelong.support.Fixtures
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthAndUserDtosTest {

    @Test
    fun `RegisterUserRequest carrega os campos do cadastro`() {
        val request = RegisterUserRequest("alice", "alice@codelong.dev", "secret123")

        assertEquals("alice", request.username)
        assertEquals("alice@codelong.dev", request.email)
        assertEquals("secret123", request.password)
    }

    @Test
    fun `LoginRequest carrega identificador e senha`() {
        val request = LoginRequest("alice", "secret123")

        assertEquals("alice", request.identifier)
        assertEquals("secret123", request.password)
    }

    @Test
    fun `ChangePasswordRequest carrega senha atual e nova`() {
        val request = ChangePasswordRequest("atual123", "nova12345")

        assertEquals("atual123", request.currentPassword)
        assertEquals("nova12345", request.newPassword)
    }

    @Test
    fun `UserResponse converte o usuario`() {
        val user = Fixtures.user(id = "u-1", username = "alice")

        val response = UserResponse.from(user)

        assertEquals("u-1", response.id)
        assertEquals("alice", response.username)
        assertEquals("alice@codelong.dev", response.email)
        assertEquals("USER", response.role)
        assertEquals(Fixtures.NOW, response.createdAt)
    }

    @Test
    fun `UserResponse converte administrador`() {
        val admin = Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN)

        assertEquals("ADMIN", UserResponse.from(admin).role)
    }

    @Test
    fun `AuthResponse usa o tipo Bearer`() {
        val result = AuthenticationResult(Fixtures.user(id = "u-1", username = "alice"), "jwt-token")

        val response = AuthResponse.from(result)

        assertEquals("jwt-token", response.token)
        assertEquals("Bearer", response.tokenType)
        assertEquals("alice", response.user.username)
        assertEquals("u-1", response.user.id)
    }

    @Test
    fun `ChangeUserStatusRequest carrega a flag active`() {
        assertEquals(true, ChangeUserStatusRequest(active = true).active)
        assertEquals(false, ChangeUserStatusRequest(active = false).active)
    }

    @Test
    fun `AdminUserResponse converte usuario ativo`() {
        val user = Fixtures.user(id = "u-1", username = "alice")

        val response = AdminUserResponse.from(user)

        assertEquals("u-1", response.id)
        assertEquals("alice", response.username)
        assertEquals("alice@codelong.dev", response.email)
        assertEquals("USER", response.role)
        assertEquals("ACTIVE", response.status)
        assertEquals(Fixtures.NOW, response.createdAt)
        assertEquals(Fixtures.NOW, response.updatedAt)
    }

    @Test
    fun `AdminUserResponse converte usuario inativo`() {
        val user = Fixtures.user(id = "u-1").deactivate(TestClock.fixed.instant())

        val response = AdminUserResponse.from(user)

        assertEquals("INACTIVE", response.status)
        assertEquals(Fixtures.NOW, response.updatedAt)
    }

    @Test
    fun `AdminUserResponse converte administrador`() {
        val admin = Fixtures.user(id = "a-1", username = "admin", role = Role.ADMIN)

        assertEquals("ADMIN", AdminUserResponse.from(admin).role)
    }
}
