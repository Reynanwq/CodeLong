package com.codelong.domain.exception

import com.codelong.domain.exception.DomainException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class DomainExceptionsTest {

    companion object {

        @JvmStatic
        fun erros(): List<Arguments> = listOf(
            Arguments.of(DomainException.invalidInput("C", "m"), ErrorKind.INVALID_INPUT, "C"),
            Arguments.of(DomainException.unauthorized("C", "m"), ErrorKind.UNAUTHORIZED, "C"),
            Arguments.of(DomainException.forbidden("C", "m"), ErrorKind.FORBIDDEN, "C"),
            Arguments.of(DomainException.notFound("C", "m"), ErrorKind.NOT_FOUND, "C"),
            Arguments.of(DomainException.conflict("C", "m"), ErrorKind.CONFLICT, "C"),
            Arguments.of(DomainException.unprocessable("C", "m"), ErrorKind.UNPROCESSABLE, "C"),
            Arguments.of(
                DomainException.concurrentModification(),
                ErrorKind.CONCURRENT_MODIFICATION,
                "CONCURRENT_MODIFICATION"
            )
        )
    }

    @Test
    fun `DomainException expoe codigo, tipo e mensagem`() {
        val exception = DomainException("SOME_CODE", ErrorKind.INVALID_INPUT, "Mensagem segura")

        assertEquals("SOME_CODE", exception.code)
        assertEquals(ErrorKind.INVALID_INPUT, exception.kind)
        assertEquals("Mensagem segura", exception.message)
    }

    @Test
    fun `DomainException e uma RuntimeException`() {
        assertInstanceOf(RuntimeException::class.java, DomainException("C", ErrorKind.CONFLICT, "m"))
    }

    @Test
    fun `invalidInput preserva codigo e mensagem`() {
        val exception = DomainException.invalidInput("INVALID", "Entrada invalida")

        assertEquals("INVALID", exception.code)
        assertEquals("Entrada invalida", exception.message)
    }

    @Test
    fun `notFound preserva codigo e mensagem`() {
        val exception = DomainException.notFound("NOT_FOUND", "Nao encontrado")

        assertEquals("NOT_FOUND", exception.code)
        assertEquals("Nao encontrado", exception.message)
    }

    @Test
    fun `forbidden preserva codigo e mensagem`() {
        val exception = DomainException.forbidden("FORBIDDEN", "Sem permissao")

        assertEquals("FORBIDDEN", exception.code)
        assertEquals("Sem permissao", exception.message)
    }

    @Test
    fun `unauthorized preserva codigo e mensagem`() {
        val exception = DomainException.unauthorized("UNAUTHORIZED", "Nao autenticado")

        assertEquals("UNAUTHORIZED", exception.code)
        assertEquals("Nao autenticado", exception.message)
    }

    @Test
    fun `conflict preserva codigo e mensagem`() {
        val exception = DomainException.conflict("CONFLICT", "Conflito")

        assertEquals("CONFLICT", exception.code)
        assertEquals("Conflito", exception.message)
    }

    @Test
    fun `unprocessable preserva codigo e mensagem`() {
        val exception = DomainException.unprocessable("UNPROCESSABLE", "Nao processavel")

        assertEquals("UNPROCESSABLE", exception.code)
        assertEquals("Nao processavel", exception.message)
    }

    @Test
    fun `concurrentModification usa codigo e mensagem padrao`() {
        val exception = DomainException.concurrentModification()

        assertEquals(DomainException.CONCURRENT_MODIFICATION_CODE, exception.code)
        assertEquals(DomainException.CONCURRENT_MODIFICATION_MESSAGE, exception.message)
        assertEquals("CONCURRENT_MODIFICATION", exception.code)
        assertEquals(
            "The game was modified concurrently. Reload the current state and try again.",
            exception.message
        )
    }

    @Test
    fun `ErrorKind cobre todas as classificacoes esperadas`() {
        assertEquals(
            listOf(
                ErrorKind.INVALID_INPUT,
                ErrorKind.UNAUTHORIZED,
                ErrorKind.FORBIDDEN,
                ErrorKind.NOT_FOUND,
                ErrorKind.CONFLICT,
                ErrorKind.CONCURRENT_MODIFICATION,
                ErrorKind.UNPROCESSABLE
            ),
            ErrorKind.entries
        )
    }

    @ParameterizedTest
    @MethodSource("erros")
    fun `cada fabrica produz o ErrorKind e o codigo correspondentes`(
        exception: DomainException,
        kind: ErrorKind,
        code: String
    ) {
        assertEquals(kind, exception.kind)
        assertEquals(code, exception.code)
        assertTrue(exception.message.isNotBlank())
    }

    @ParameterizedTest
    @MethodSource("erros")
    fun `todas as falhas podem ser capturadas como DomainException`(
        exception: DomainException,
        kind: ErrorKind,
        code: String
    ) {
        val captured = runCatching { throw exception }.exceptionOrNull()

        assertInstanceOf(DomainException::class.java, captured)
        assertEquals(code, (captured as DomainException).code)
        assertEquals(kind, captured.kind)
    }

    @ParameterizedTest
    @MethodSource("erros")
    fun `todas as falhas herdam de RuntimeException`(
        exception: DomainException,
        kind: ErrorKind,
        code: String
    ) {
        assertInstanceOf(RuntimeException::class.java, exception)
        assertTrue(exception.code.isNotBlank())
        assertTrue(exception.message.isNotBlank())
        assertEquals(kind, exception.kind)
        assertEquals(code, exception.code)
    }
}
