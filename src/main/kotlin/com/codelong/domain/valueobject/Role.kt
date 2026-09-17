package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors

enum class Role {
    USER,
    ADMIN;

    companion object {
        fun fromName(name: String): Role =
            entries.firstOrNull { it.name == name.uppercase() }
                ?: throw Errors.unknownRole(name)
    }
}