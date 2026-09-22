package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors

/**
 * Modo de jogo, que define a ordem das perguntas na partida.
 *
 * - [CLASSIC]: dificuldade sempre crescente (nivel 1 .. 10), embaralhando
 *   apenas dentro do mesmo nivel.
 * - [GENOCIDA]: ordem totalmente aleatoria, sem respeitar dificuldade.
 * - [APRENDIZADO]: perguntas de um unico tema (categoria), em dificuldade
 *   crescente; morte subita como no genocida (resposta errada encerra).
 * - [GUBEE]: perguntas exclusivas da categoria GUBEE (nao usadas nos demais
 *   modos), em dificuldade crescente, sem morte subita. Ranking proprio.
 */
enum class GameMode {
    CLASSIC,
    GENOCIDA,
    APRENDIZADO,
    GUBEE;

    companion object {
        fun fromName(name: String): GameMode =
            entries.firstOrNull { it.name == name.trim().uppercase() }
                ?: throw Errors.unknownGameMode(name)
    }
}
