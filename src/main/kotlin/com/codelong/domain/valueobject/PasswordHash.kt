package com.codelong.domain.valueobject

@JvmInline
value class PasswordHash(val value: String) {

    init {
        require(value.isNotBlank()) {
            BLANK_MESSAGE
        }
    }

    companion object {
        const val BLANK_MESSAGE = "password hash must not be blank"
    }
}