package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors



@JvmInline
value class Email private constructor(val value: String) {

    init {
        require(isValid(value)) {
            throw Errors.emailInvalid()
        }
    }

    companion object {
        private const val PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

        fun of(raw: String): Email = Email(raw.trim().lowercase())

        fun isValid(raw: String): Boolean = pattern().matches(raw.trim().lowercase())

        private fun pattern(): Regex = Regex(PATTERN)
    }
}