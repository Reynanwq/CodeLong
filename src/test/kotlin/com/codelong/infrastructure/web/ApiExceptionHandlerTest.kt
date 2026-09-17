package com.codelong.infrastructure.web

import com.codelong.domain.exception.ConcurrentGameModificationException
import com.codelong.domain.exception.ConflictException
import com.codelong.domain.exception.DomainException
import com.codelong.domain.exception.ForbiddenException
import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.exception.UnauthorizedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.MethodParameter
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException

class ApiExceptionHandlerTest {

    private val handler = ApiExceptionHandler()

    data class SamplePayload(val name: String)

    fun sampleEndpoint(payload: SamplePayload) = payload

    companion object {

        @JvmStatic
        fun excecoesComStatus(): List<Arguments> = listOf(
            Arguments.of(NotFoundException("USER_NOT_FOUND", "nao encontrado"), HttpStatus.NOT_FOUND),
            Arguments.of(ConflictException("USERNAME_ALREADY_EXISTS", "conflito"), HttpStatus.CONFLICT),
            Arguments.of(ConcurrentGameModificationException(), HttpStatus.CONFLICT),
            Arguments.of(InvalidInputException("answer.option.invalid", "entrada invalida"), HttpStatus.BAD_REQUEST),
            Arguments.of(ForbiddenException("GAME_ACCESS_DENIED", "sem permissao"), HttpStatus.FORBIDDEN),
            Arguments.of(UnauthorizedException("INVALID_CREDENTIALS", "nao autenticado"), HttpStatus.UNAUTHORIZED),
            Arguments.of(DomainException("CODIGO_DESCONHECIDO", "nao mapeado"), HttpStatus.UNPROCESSABLE_CONTENT)
        )
    }

    @ParameterizedTest
    @MethodSource("excecoesComStatus")
    fun `mapeia cada excecao de dominio para o status correto`(
        exception: DomainException,
        expected: HttpStatus
    ) {
        val response = handler.handleDomain(exception)

        assertEquals(expected, response.statusCode)
        assertEquals(exception.code, response.body?.code)
        assertEquals(exception.message, response.body?.message)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `excecao de dominio desconhecida resulta em 422`() {
        val response = handler.handleDomain(DomainException("QUALQUER", "mensagem"))

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.statusCode)
        assertEquals("QUALQUER", response.body?.code)
    }

    @Test
    fun `erro de validacao resulta em 400 com os campos invalidos`() {
        val method = ApiExceptionHandlerTest::class.java
            .getDeclaredMethod("sampleEndpoint", SamplePayload::class.java)
        val parameter = MethodParameter(method, 0)
        val binding = BeanPropertyBindingResult(SamplePayload(""), "payload")
        binding.rejectValue("name", "NotBlank", "name is required")
        val exception = MethodArgumentNotValidException(parameter, binding)

        val response = handler.handleValidation(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("VALIDATION_ERROR", response.body?.code)
        assertEquals("name: name is required", response.body?.message)
    }

    @Test
    fun `erro de validacao sem campos resulta em mensagem padrao`() {
        val method = ApiExceptionHandlerTest::class.java
            .getDeclaredMethod("sampleEndpoint", SamplePayload::class.java)
        val parameter = MethodParameter(method, 0)
        val binding = BeanPropertyBindingResult(SamplePayload("ok"), "payload")
        val exception = MethodArgumentNotValidException(parameter, binding)

        val response = handler.handleValidation(exception)

        assertEquals("Invalid request", response.body?.message)
    }

    @Test
    fun `corpo ilegivel resulta em 400`() {
        val exception = HttpMessageNotReadableException(
            "json invalido",
            MockHttpInputMessage(ByteArray(0))
        )

        val response = handler.handleUnreadable(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("MALFORMED_REQUEST", response.body?.code)
        assertEquals("The request body is invalid or malformed", response.body?.message)
    }

    @Test
    fun `argumento ilegal resulta em 400`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("size must be between 1 and 100"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_ARGUMENT", response.body?.code)
        assertEquals("size must be between 1 and 100", response.body?.message)
    }

    @Test
    fun `argumento ilegal sem mensagem usa texto padrao`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException())

        assertEquals("Invalid argument", response.body?.message)
    }

    @Test
    fun `chave duplicada resulta em 409`() {
        val response = handler.handleDuplicate(DuplicateKeyException("duplicado"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("DUPLICATE_KEY", response.body?.code)
        assertEquals("A conflicting resource already exists", response.body?.message)
    }

    @Test
    fun `erro inesperado resulta em 500 sem detalhes internos`() {
        val response = handler.handleUnexpected(RuntimeException("detalhe interno sensivel"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.code)
        assertEquals("An unexpected error occurred", response.body?.message)
    }

    @Test
    fun `resposta de erro sempre carrega timestamp`() {
        val response = handler.handleUnexpected(RuntimeException("falha"))

        assertNotNull(response.body?.timestamp)
    }
}
