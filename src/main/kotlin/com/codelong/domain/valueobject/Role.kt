package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException

enum class Role {
    USER,
    ADMIN;

    companion object {
        fun fromName(name: String): Role =
            entries.firstOrNull { it.name == name.uppercase() }
                ?: throw com.codelong.domain.exception.DomainException.invalidInput(
                    "role.invalid",
                    "Unknown role: $name"
                )
    }
}