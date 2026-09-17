package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException


enum class GameStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED;

    companion object {
        fun fromName(name: String): GameStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw DomainException.invalidInput("status.invalid", "Unknown game status: $name")
    }
}
