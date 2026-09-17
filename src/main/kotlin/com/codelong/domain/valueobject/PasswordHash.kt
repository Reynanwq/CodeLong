package com.codelong.domain.valueobject

@JvmInline
value class PasswordHash(val value: String) {

    init {
        require(value.isNotBlank()) {
            "password hash must not be blank"
        }
    }
}