package com.codelong.domain.valueobject

import com.codelong.domain.exception.InvalidInputException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UsernameTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "abc",
            "ABC",
            "AbC",
            "a_1",
            "a-1",
            "a.1",
            "user_name",
            "user-name",
            "user.name",
            "user_name-1.x",
            "___",
            "...",
            "---",
            "123",
            "abcdefghijklmnopqrst",
            "a234567890123456789",
            "padded"
        ]
    )
    fun `aceita usernames validos`(raw: String) {
        assertTrue(Username.isValid(raw))

        val username = Username.of(raw)

        assertEquals(raw.trim(), username.value)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "\t\n",
            "a",
            "ab",
            "abcdefghijklmnopqrstu",
            "com espaço",
            "usuário",
            "user@name",
            "user#1",
            "user!",
            "user+1",
            "user/1",
            "user\\1",
            "user,name",
            "user;name",
            "user:name",
            "user'name",
            "user\"name",
            "user(name)",
            "user[name]",
            "user{name}",
            "user|name",
            "user?name",
            "user*name",
            "user%name",
            "user&name",
            "user\$name",
            "user=name",
            "user~name",
            "user`name",
            "user<name>"
        ]
    )
    fun `rejeita usernames invalidos`(raw: String) {
        assertFalse(Username.isValid(raw))

        val error = assertThrows<InvalidInputException> { Username.of(raw) }

        assertEquals("username.invalid", error.code)
        assertEquals(
            "Username must have between 3 and 20 characters using only letters, numbers, '_' or '-'",
            error.message
        )
    }

    @Test
    fun `preserva a caixa original`() {
        assertEquals("Alice", Username.of("Alice").value)
    }

    @Test
    fun `remove espacos nas extremidades`() {
        assertEquals("alice", Username.of("  alice  ").value)
        assertTrue(Username.isValid("  alice  "))
    }

    @Test
    fun `limites de tamanho sao inclusivos`() {
        assertTrue(Username.isValid("abc"))
        assertTrue(Username.isValid("abcdefghijklmnopqrst"))
        assertFalse(Username.isValid("ab"))
        assertFalse(Username.isValid("abcdefghijklmnopqrstu"))
    }
}
