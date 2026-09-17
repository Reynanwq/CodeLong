package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors



@JvmInline
value class Username private constructor(val value: String) {

    init {
        require(isValid(value)) {
            throw Errors.usernameInvalid()
        }
    }

    companion object {
        private const val PATTERN = "^[A-Za-z0-9_.-]{3,20}$"

        fun of(raw: String): Username = Username(raw.trim())

        fun isValid(raw: String): Boolean = pattern().matches(raw.trim())

        private fun pattern(): Regex = Regex(PATTERN)
    }
}