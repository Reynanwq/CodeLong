package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException


enum class AccountStatus {
    ACTIVE,
    INACTIVE;

    companion object {
        fun fromName(name: String): AccountStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw DomainException.invalidInput("status.invalid", "Unknown account status: $name")
    }
}
