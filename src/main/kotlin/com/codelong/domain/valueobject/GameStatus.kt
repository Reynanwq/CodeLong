package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors



enum class GameStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED;

    companion object {
        fun fromName(name: String): GameStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw Errors.unknownGameStatus(name)
    }
}
