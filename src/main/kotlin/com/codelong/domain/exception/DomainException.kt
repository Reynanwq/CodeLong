package com.codelong.domain.exception

/**
 * Base de todas as excecoes de dominio.
 *
 * @param code   codigo de erro estavel e legivel (ex.: USER_NOT_FOUND)
 * @param message mensagem legivel, segura para envio ao cliente
 */
open class DomainException(
    val code: String,
    override val message: String
) : RuntimeException(message)