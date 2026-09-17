package com.codelong.domain.valueobject

import com.codelong.domain.exception.InvalidInputException

@JvmInline
value class Username private constructor(val value: String) {

    init {
        require(isValid(value)) {
            throw InvalidInputException(
                "username.invalid",
                "Username must have between 3 and 20 characters using only letters, numbers, '_' or '-'"
            )
        }
    }

    companion object {
        private val PATTERN = Regex("^[A-Za-z0-9_.-]{3,20}$")

        fun of(raw: String): Username = Username(raw.trim())

        fun isValid(raw: String): Boolean = PATTERN.matches(raw.trim())
    }
}