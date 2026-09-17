package com.codelong.application.service

import com.codelong.domain.exception.InvalidInputException

/**
 * Politica de senha compartilhada pelo cadastro e pela troca de senha.
 *
 * Centraliza o comprimento minimo e o limite de bytes do BCrypt, evitando que
 * a regra seja duplicada entre casos de uso.
 */
class PasswordPolicy(
    private val minLength: Int = MIN_LENGTH,
    private val maxLength: Int = MAX_LENGTH
) {

    fun requireStrong(plainPassword: String) {
        if (plainPassword.length < minLength) {
            throw InvalidInputException(
                "password.tooWeak",
                "Password must have at least $minLength characters"
            )
        }
        if (plainPassword.length > maxLength) {
            throw InvalidInputException(
                "password.tooWeak",
                "Password must have at most $maxLength characters"
            )
        }
    }

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 72
    }
}
