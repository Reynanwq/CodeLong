package com.codelong.domain.exception

/**
 * Falha de dominio.
 *
 * Nao existe hierarquia de classes: [kind] classifica o erro e a borda decide o
 * status HTTP por composicao, sem heranca e sem if/else.
 */
class DomainException(
    val code: String,
    val kind: ErrorKind,
    override val message: String
) : RuntimeException(message) {

    companion object {

        fun invalidInput(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.INVALID_INPUT, message)

        fun unauthorized(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.UNAUTHORIZED, message)

        fun forbidden(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.FORBIDDEN, message)

        fun notFound(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.NOT_FOUND, message)

        fun conflict(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.CONFLICT, message)

        fun unprocessable(code: String, message: String): DomainException =
            DomainException(code, ErrorKind.UNPROCESSABLE, message)

        fun concurrentModification(): DomainException = DomainException(
            code = CONCURRENT_MODIFICATION_CODE,
            kind = ErrorKind.CONCURRENT_MODIFICATION,
            message = CONCURRENT_MODIFICATION_MESSAGE
        )

        const val CONCURRENT_MODIFICATION_CODE = "CONCURRENT_MODIFICATION"
        const val CONCURRENT_MODIFICATION_MESSAGE =
            "The game was modified concurrently. Reload the current state and try again."
    }
}

/**
 * Classificacao do erro de dominio, usada pela borda para escolher o status HTTP.
 */
enum class ErrorKind {
    INVALID_INPUT,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    CONCURRENT_MODIFICATION,
    UNPROCESSABLE
}
