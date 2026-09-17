package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException


enum class AccountStatus {
    ACTIVE,
    INACTIVE;

    companion object {

        /** Converte a flag de ativacao no status correspondente, sem ramificacao. */
        fun of(active: Boolean): AccountStatus = mapOf(true to ACTIVE, false to INACTIVE).getValue(active)

        fun fromName(name: String): AccountStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw DomainException.invalidInput("status.invalid", "Unknown account status: $name")
    }
}
