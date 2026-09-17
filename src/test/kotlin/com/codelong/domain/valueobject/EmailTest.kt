package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EmailTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "alice@codelong.dev",
            "a@b.co",
            "user.name+tag@example.com",
            "u_1%x-y@sub.domain.com.br",
            "ALICE@CODELONG.DEV",
            "Alice.Bob@Example.COM",
            "user123@example123.io",
            "user-100@example.com",
            "user_name@example.com",
            "first.last@example.museum",
            "x@y.zz",
            "1@2.ab",
            "a.b.c.d@e.f.gh",
            "user%percent@example.com",
            "user+plus@example.com",
            "user-dash@my-domain.com",
            "user_under@my-domain.com",
            "mixed.Case+Tag@Sub.Domain.COM"
        ]
    )
    fun `aceita emails validos`(raw: String) {
        assertTrue(Email.isValid(raw))

        val email = Email.of(raw)

        assertEquals(raw.trim().lowercase(), email.value)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "\t",
            "sem-arroba",
            "@example.com",
            "user@",
            "user@example",
            "user@example.c",
            "user@@example.com",
            "user name@example.com",
            "user@exa mple.com",
            "user@exa_mple.com",
            "user@example.1com",
            " usuario @example.com",
            "user@example.com.br extra",
            "user#name@example.com",
            "user@exam!ple.com",
            "a@b",
            "a@.com"
        ]
    )
    fun `rejeita emails invalidos`(raw: String) {
        assertFalse(Email.isValid(raw))

        val error = assertThrows<DomainException> { Email.of(raw) }

        assertEquals("email.invalid", error.code)
        assertEquals("The email address is invalid", error.message)
    }

    @Test
    fun `normaliza removendo espacos e caixa alta`() {
        assertEquals("alice@codelong.dev", Email.of("  ALICE@CodeLong.DEV  ").value)
    }

    @Test
    fun `isValid considera espacos nas extremidades`() {
        assertTrue(Email.isValid("  alice@codelong.dev  "))
    }

    @Test
    fun `emails normalizados sao iguais`() {
        assertEquals(Email.of("Alice@Example.com"), Email.of("alice@example.com"))
        assertEquals(Email.of("alice@example.com").hashCode(), Email.of("ALICE@EXAMPLE.COM").hashCode())
    }
}
