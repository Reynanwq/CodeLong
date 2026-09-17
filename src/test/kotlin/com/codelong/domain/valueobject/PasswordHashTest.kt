package com.codelong.domain.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PasswordHashTest {

    @ParameterizedTest
    @ValueSource(strings = ["x", "hashed:secret123", "\$2a\$10\$abcdefghijklmnopqrstuv", "12345678", " x"])
    fun `aceita hash nao vazio`(raw: String) {
        assertEquals(raw, PasswordHash(raw).value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "   ", "\t", "\n", "\t\n ", "\r\n"])
    fun `rejeita hash em branco`(raw: String) {
        val error = assertThrows<IllegalArgumentException> { PasswordHash(raw) }

        assertEquals("password hash must not be blank", error.message)
    }

    @Test
    fun `igualdade por valor`() {
        assertEquals(PasswordHash("hash"), PasswordHash("hash"))
        assertEquals(PasswordHash("hash").hashCode(), PasswordHash("hash").hashCode())
        assertNotEquals(PasswordHash("hash"), PasswordHash("outro"))
    }
}
