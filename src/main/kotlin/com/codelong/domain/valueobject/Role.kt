package com.codelong.domain.valueobject

enum class Role {
    USER,
    ADMIN;

    companion object {
        fun fromName(name: String): Role =
            entries.firstOrNull { it.name == name.uppercase() }
                ?: throw com.codelong.domain.exception.InvalidInputException(
                    "role.invalid",
                    "Unknown role: $name"
                )
    }
}