package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors

/**
 * Modo de jogo, que define a ordem das perguntas na partida.
 *
 * - [CLASSIC]: dificuldade sempre crescente (nivel 1 .. 10), embaralhando
 *   apenas dentro do mesmo nivel.
 * - [GENOCIDA]: ordem totalmente aleatoria, sem respeitar dificuldade.
 */
enum class GameMode {
    CLASSIC,
    GENOCIDA;

    companion object {
        fun fromName(name: String): GameMode =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw Errors.unknownGameMode(name)
    }
}
