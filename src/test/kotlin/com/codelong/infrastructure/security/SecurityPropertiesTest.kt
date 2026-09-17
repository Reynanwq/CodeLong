package com.codelong.infrastructure.security

import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class SecurityPropertiesTest {

    @Test
    fun `usa os valores padrao`() {
        val properties = SecurityProperties()

        assertEquals("", properties.jwt.secret)
        assertEquals(Duration.ofHours(8), properties.jwt.expiration)
        assertTrue(properties.cors.allowedOrigins.isEmpty())
        assertEquals("", properties.admin.username)
        assertEquals("", properties.admin.password)
    }

    @Test
    fun `aceita valores customizados`() {
        val properties = SecurityProperties(
            jwt = SecurityProperties.Jwt(secret = "segredo", expiration = Duration.ofMinutes(30)),
            cors = SecurityProperties.Cors(allowedOrigins = listOf("http://localhost:3000")),
            admin = SecurityProperties.Admin(username = "admin", password = "admin12345")
        )

        assertEquals("segredo", properties.jwt.secret)
        assertEquals(Duration.ofMinutes(30), properties.jwt.expiration)
        assertEquals(listOf("http://localhost:3000"), properties.cors.allowedOrigins)
        assertEquals("admin", properties.admin.username)
        assertEquals("admin12345", properties.admin.password)
    }

    @Test
    fun `Jwt tem igualdade por valor`() {
        assertEquals(
            SecurityProperties.Jwt("segredo", Duration.ofHours(1)),
            SecurityProperties.Jwt("segredo", Duration.ofHours(1))
        )
        assertNotEquals(
            SecurityProperties.Jwt("segredo", Duration.ofHours(1)),
            SecurityProperties.Jwt("outro", Duration.ofHours(1))
        )
    }

    @Test
    fun `Cors tem igualdade por valor`() {
        assertEquals(
            SecurityProperties.Cors(listOf("a", "b")),
            SecurityProperties.Cors(listOf("a", "b"))
        )
        assertNotEquals(
            SecurityProperties.Cors(listOf("a")),
            SecurityProperties.Cors(listOf("b"))
        )
    }

    @Test
    fun `Admin tem igualdade por valor`() {
        assertEquals(
            SecurityProperties.Admin("admin", "senha"),
            SecurityProperties.Admin("admin", "senha")
        )
        assertNotEquals(
            SecurityProperties.Admin("admin", "senha"),
            SecurityProperties.Admin("admin", "outra")
        )
    }

    @Test
    fun `Cors aceita multiplas origens`() {
        val cors = SecurityProperties.Cors(listOf("http://a", "http://b", "http://c"))

        assertEquals(3, cors.allowedOrigins.size)
    }

    @Test
    fun `AuthenticatedUser carrega usuario e papel`() {
        val principal = AuthenticatedUser(UserId("u-1"), Role.ADMIN)

        assertEquals(UserId("u-1"), principal.userId)
        assertEquals(Role.ADMIN, principal.role)
    }

    @Test
    fun `AuthenticatedUser tem igualdade por valor`() {
        assertEquals(
            AuthenticatedUser(UserId("u-1"), Role.USER),
            AuthenticatedUser(UserId("u-1"), Role.USER)
        )
        assertNotEquals(
            AuthenticatedUser(UserId("u-1"), Role.USER),
            AuthenticatedUser(UserId("u-1"), Role.ADMIN)
        )
        assertNotEquals(
            AuthenticatedUser(UserId("u-1"), Role.USER),
            AuthenticatedUser(UserId("u-2"), Role.USER)
        )
    }

    @Test
    fun `AuthenticatedUser pode ser copiado`() {
        val principal = AuthenticatedUser(UserId("u-1"), Role.USER)

        assertEquals(AuthenticatedUser(UserId("u-1"), Role.ADMIN), principal.copy(role = Role.ADMIN))
    }
}
