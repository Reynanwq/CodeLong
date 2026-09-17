package com.codelong.domain.valueobject

import com.codelong.domain.exception.InvalidInputException

enum class AccountStatus {
    ACTIVE,
    INACTIVE;

    companion object {
        fun fromName(name: String): AccountStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw InvalidInputException("status.invalid", "Unknown account status: $name")
    }
}
