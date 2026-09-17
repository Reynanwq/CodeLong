package com.codelong.application.service

import com.codelong.application.service.DefaultPasswordPolicy

import com.codelong.domain.exception.DomainException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PasswordPolicyTest {

    private val policy = DefaultPasswordPolicy()

    @Test
    fun `limites padrao sao oito e setenta e dois`() {
        assertEquals(8, PasswordPolicy.MIN_LENGTH)
        assertEquals(72, PasswordPolicy.MAX_LENGTH)
    }

    @Test
    fun `aceita senha com exatamente o tamanho minimo`() {
        assertDoesNotThrow { policy.requireStrong("a".repeat(8)) }
    }

    @Test
    fun `aceita senha com exatamente o tamanho maximo`() {
        assertDoesNotThrow { policy.requireStrong("a".repeat(72)) }
    }

    @Test
    fun `aceita senha longa dentro do limite`() {
        assertDoesNotThrow { policy.requireStrong("senha-muito-segura-123") }
    }

    @Test
    fun `aceita senha apenas com espacos desde que tenha tamanho minimo`() {
        assertDoesNotThrow { policy.requireStrong("        ") }
    }

    @Test
    fun `aceita senha com caracteres especiais e unicode`() {
        assertDoesNotThrow { policy.requireStrong("çã@#$%¨&*()_+😀😀") }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `rejeita senha abaixo do minimo`(length: Int) {
        val error = assertThrows<DomainException> { policy.requireStrong("a".repeat(length)) }

        assertEquals("password.tooWeak", error.code)
        assertEquals("Password must have at least 8 characters", error.message)
    }

    @ParameterizedTest
    @ValueSource(ints = [73, 74, 80, 100, 200])
    fun `rejeita senha acima do maximo`(length: Int) {
        val error = assertThrows<DomainException> { policy.requireStrong("a".repeat(length)) }

        assertEquals("password.tooWeak", error.code)
        assertEquals("Password must have at most 72 characters", error.message)
    }

    @Test
    fun `rejeita senha vazia`() {
        val error = assertThrows<DomainException> { policy.requireStrong("") }

        assertEquals("password.tooWeak", error.code)
    }

    @Test
    fun `politica customizada usa os limites informados`() {
        val custom = DefaultPasswordPolicy(minLength = 3, maxLength = 5)

        assertDoesNotThrow { custom.requireStrong("abc") }
        assertDoesNotThrow { custom.requireStrong("abcde") }
        assertThrows<DomainException> { custom.requireStrong("ab") }
        assertThrows<DomainException> { custom.requireStrong("abcdef") }
    }

    @Test
    fun `politica customizada reporta os proprios limites na mensagem`() {
        val custom = DefaultPasswordPolicy(minLength = 10, maxLength = 20)

        val tooShort = assertThrows<DomainException> { custom.requireStrong("curta") }
        val tooLong = assertThrows<DomainException> { custom.requireStrong("x".repeat(21)) }

        assertTrue(tooShort.message.contains("10"))
        assertTrue(tooLong.message.contains("20"))
    }
}
