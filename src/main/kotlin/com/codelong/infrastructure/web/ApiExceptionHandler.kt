package com.codelong.infrastructure.web

import com.codelong.domain.exception.DomainException
import com.codelong.domain.exception.ErrorKind
import com.codelong.infrastructure.web.dto.ApiErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    private val statusByKind: Map<ErrorKind, HttpStatus> = mapOf(
        ErrorKind.INVALID_INPUT to HttpStatus.BAD_REQUEST,
        ErrorKind.UNAUTHORIZED to HttpStatus.UNAUTHORIZED,
        ErrorKind.FORBIDDEN to HttpStatus.FORBIDDEN,
        ErrorKind.NOT_FOUND to HttpStatus.NOT_FOUND,
        ErrorKind.CONFLICT to HttpStatus.CONFLICT,
        ErrorKind.CONCURRENT_MODIFICATION to HttpStatus.CONFLICT,
        ErrorKind.UNPROCESSABLE to HttpStatus.UNPROCESSABLE_CONTENT
    )

    @ExceptionHandler(DomainException::class)
    fun handleDomain(ex: DomainException): ResponseEntity<ApiErrorResponse> =
        build(statusOf(ex), ex.code, ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Invalid request" }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is invalid or malformed")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.message ?: "Invalid argument")

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicate(ex: DuplicateKeyException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.CONFLICT, "DUPLICATE_KEY", "A conflicting resource already exists")

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Falha inesperada", ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred")
    }

    private fun statusOf(ex: DomainException): HttpStatus = statusByKind.getValue(ex.kind)

    private fun build(status: HttpStatus, code: String, message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse.of(code, message))
}
