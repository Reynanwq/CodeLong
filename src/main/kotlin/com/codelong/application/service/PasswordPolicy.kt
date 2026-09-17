package com.codelong.application.service

import com.codelong.domain.exception.Errors

/**
 * Politica de senha compartilhada pelo cadastro e pela troca de senha.
 *
 * Centraliza o comprimento minimo e o limite de bytes do BCrypt, evitando que
 * a regra seja duplicada entre casos de uso.
 */
interface PasswordPolicy {

    fun requireStrong(plainPassword: String)

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 72
    }
}

class DefaultPasswordPolicy(
    private val minLength: Int = PasswordPolicy.MIN_LENGTH,
    private val maxLength: Int = PasswordPolicy.MAX_LENGTH
) : PasswordPolicy {

    override fun requireStrong(plainPassword: String) {
        when {
            plainPassword.length < minLength -> throw Errors.passwordTooShort(minLength)
            plainPassword.length > maxLength -> throw Errors.passwordTooLong(maxLength)
        }
    }
}
