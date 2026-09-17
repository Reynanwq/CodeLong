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

private const val FIELD_SEPARATOR = "; "
private const val FIELD_TEMPLATE = "%s: %s"
private const val INVALID_REQUEST = "Invalid request"
private const val VALIDATION_ERROR = "VALIDATION_ERROR"
private const val MALFORMED_REQUEST = "MALFORMED_REQUEST"
private const val MALFORMED_REQUEST_MESSAGE = "The request body is invalid or malformed"
private const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
private const val INVALID_ARGUMENT_MESSAGE = "Invalid argument"
private const val DUPLICATE_KEY = "DUPLICATE_KEY"
private const val DUPLICATE_KEY_MESSAGE = "A conflicting resource already exists"
private const val UNEXPECTED_ERROR_LOG = "Falha inesperada"
private const val INTERNAL_ERROR = "INTERNAL_ERROR"
private const val INTERNAL_ERROR_MESSAGE = "An unexpected error occurred"

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
            .joinToString(FIELD_SEPARATOR) { FIELD_TEMPLATE.format(it.field, it.defaultMessage) }
            .ifBlank { INVALID_REQUEST }
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST, MALFORMED_REQUEST_MESSAGE)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, INVALID_ARGUMENT, ex.message ?: INVALID_ARGUMENT_MESSAGE)

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicate(ex: DuplicateKeyException): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.CONFLICT, DUPLICATE_KEY, DUPLICATE_KEY_MESSAGE)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error(UNEXPECTED_ERROR_LOG, ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, INTERNAL_ERROR_MESSAGE)
    }

    private fun statusOf(ex: DomainException): HttpStatus = statusByKind.getValue(ex.kind)

    private fun build(status: HttpStatus, code: String, message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse.of(code, message))
}
