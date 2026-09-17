package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException


@JvmInline
value class Email private constructor(val value: String) {

    init {
        require(isValid(value)) {
            throw DomainException.invalidInput("email.invalid", "The email address is invalid")
        }
    }

    companion object {
        private val PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): Email = Email(raw.trim().lowercase())

        fun isValid(raw: String): Boolean = PATTERN.matches(raw.trim().lowercase())
    }
}