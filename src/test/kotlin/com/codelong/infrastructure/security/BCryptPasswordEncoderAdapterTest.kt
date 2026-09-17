package com.codelong.infrastructure.security

import com.codelong.domain.valueobject.PasswordHash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BCryptPasswordEncoderAdapterTest {

    private val encoder = BCryptPasswordEncoderAdapter()

    @ParameterizedTest
    @ValueSource(strings = ["secret123", "outra-senha-forte", "12345678", "a", "senha com espaço"])
    fun `encode gera hash diferente da senha`(raw: String) {
        val hash = encoder.encode(raw)

        assertTrue(hash.value.isNotBlank())
        assertNotEquals(raw, hash.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["secret123", "outra-senha-forte", "12345678"])
    fun `matches aceita a senha correta`(raw: String) {
        assertTrue(encoder.matches(raw, encoder.encode(raw)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["secret123", "outra-senha-forte"])
    fun `matches rejeita senha incorreta`(raw: String) {
        val hash = encoder.encode(raw)

        assertFalse(encoder.matches("senha-errada", hash))
        assertFalse(encoder.matches(raw + "x", hash))
        assertFalse(encoder.matches("", hash))
    }

    @Test
    fun `encode usa salt aleatorio`() {
        val first = encoder.encode("secret123")
        val second = encoder.encode("secret123")

        assertNotEquals(first.value, second.value)
        assertTrue(encoder.matches("secret123", first))
        assertTrue(encoder.matches("secret123", second))
    }

    @Test
    fun `hash tem o prefixo do bcrypt`() {
        val hash = encoder.encode("secret123")

        assertTrue(hash.value.startsWith("\$2"))
        assertTrue(hash.value.length >= 60)
    }

    @Test
    fun `matches com hash invalido retorna falso`() {
        assertFalse(encoder.matches("secret123", PasswordHash("nao-e-um-hash-bcrypt")))
    }

    @Test
    fun `encode e sensivel a caixa`() {
        val hash = encoder.encode("Secret123")

        assertTrue(encoder.matches("Secret123", hash))
        assertFalse(encoder.matches("secret123", hash))
    }

    @Test
    fun `encode aceita senha no limite de 72 caracteres`() {
        val raw = "x".repeat(72)

        assertTrue(encoder.matches(raw, encoder.encode(raw)))
    }

    @Test
    fun `hashs diferentes nao sao iguais entre si`() {
        assertNotEquals(encoder.encode("a").value, encoder.encode("b").value)
    }

    @Test
    fun `encode devolve PasswordHash`() {
        assertEquals(PasswordHash::class, encoder.encode("secret123")::class)
    }
}
