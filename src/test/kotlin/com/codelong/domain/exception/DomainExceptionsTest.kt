package com.codelong.domain.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DomainExceptionsTest {

    @Test
    fun `DomainException expoe codigo e mensagem`() {
        val exception = DomainException("SOME_CODE", "Mensagem segura")

        assertEquals("SOME_CODE", exception.code)
        assertEquals("Mensagem segura", exception.message)
    }

    @Test
    fun `DomainException e uma RuntimeException`() {
        assertInstanceOf(RuntimeException::class.java, DomainException("C", "m"))
    }

    @Test
    fun `ConflictException preserva codigo e mensagem`() {
        val exception = ConflictException("CONFLICT", "Conflito")

        assertEquals("CONFLICT", exception.code)
        assertEquals("Conflito", exception.message)
    }

    @Test
    fun `NotFoundException preserva codigo e mensagem`() {
        val exception = NotFoundException("NOT_FOUND", "Nao encontrado")

        assertEquals("NOT_FOUND", exception.code)
        assertEquals("Nao encontrado", exception.message)
    }

    @Test
    fun `ForbiddenException preserva codigo e mensagem`() {
        val exception = ForbiddenException("FORBIDDEN", "Sem permissao")

        assertEquals("FORBIDDEN", exception.code)
        assertEquals("Sem permissao", exception.message)
    }

    @Test
    fun `UnauthorizedException preserva codigo e mensagem`() {
        val exception = UnauthorizedException("UNAUTHORIZED", "Nao autenticado")

        assertEquals("UNAUTHORIZED", exception.code)
        assertEquals("Nao autenticado", exception.message)
    }

    @Test
    fun `InvalidInputException preserva codigo e mensagem`() {
        val exception = InvalidInputException("INVALID", "Entrada invalida")

        assertEquals("INVALID", exception.code)
        assertEquals("Entrada invalida", exception.message)
    }

    @Test
    fun `ConcurrentGameModificationException tem codigo e mensagem padrao`() {
        val exception = ConcurrentGameModificationException()

        assertEquals("CONCURRENT_MODIFICATION", exception.code)
        assertEquals(
            "The game was modified concurrently. Reload the current state and try again.",
            exception.message
        )
    }

    @Test
    fun `ConcurrentGameModificationException aceita codigo e mensagem customizados`() {
        val exception = ConcurrentGameModificationException("CUSTOM_CODE", "Mensagem customizada")

        assertEquals("CUSTOM_CODE", exception.code)
        assertEquals("Mensagem customizada", exception.message)
    }

    @Test
    fun `ConcurrentGameModificationException aceita apenas codigo customizado`() {
        val exception = ConcurrentGameModificationException("CUSTOM_CODE")

        assertEquals("CUSTOM_CODE", exception.code)
        assertTrue(exception.message.contains("modified concurrently"))
    }

    companion object {

        @JvmStatic
        fun excecoesDeDominio(): List<DomainException> = listOf(
            DomainException("D", "d"),
            ConflictException("C", "c"),
            NotFoundException("N", "n"),
            ForbiddenException("F", "f"),
            UnauthorizedException("U", "u"),
            InvalidInputException("I", "i"),
            ConcurrentGameModificationException()
        )
    }

    @ParameterizedTest
    @MethodSource("excecoesDeDominio")
    fun `todas as excecoes herdam de DomainException e RuntimeException`(exception: DomainException) {
        assertInstanceOf(DomainException::class.java, exception)
        assertInstanceOf(RuntimeException::class.java, exception)
        assertTrue(exception.code.isNotBlank())
        assertTrue(exception.message.isNotBlank())
    }

    @ParameterizedTest
    @MethodSource("excecoesDeDominio")
    fun `todas as excecoes podem ser capturadas como DomainException`(exception: DomainException) {
        val captured = runCatching { throw exception }.exceptionOrNull()

        assertInstanceOf(DomainException::class.java, captured)
        assertEquals(exception.code, (captured as DomainException).code)
    }
}
