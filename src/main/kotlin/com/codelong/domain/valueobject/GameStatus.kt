package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors

/**
 * Estados possiveis de uma partida.
 *
 * - [IN_PROGRESS]: em andamento.
 * - [COMPLETED]: todas as perguntas foram respondidas.
 * - [ABANDONED]: o jogador encerrou antes do fim.
 * - [DEFEATED]: o jogador errou (ou estourou o tempo) no modo GENOCIDA, que e
 *   de morte subita.
 */
enum class GameStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
    DEFEATED;

    companion object {
        fun fromName(name: String): GameStatus =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw Errors.unknownGameStatus(name)
    }
}
