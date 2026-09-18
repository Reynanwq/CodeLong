package com.codelong.domain.port

import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface RankingRepository {
    /**
     * Ranking paginado com **todas as tentativas** elegiveis (concluidas,
     * abandonadas ou derrotadas), ordenado pela politica de desempate do
     * dominio. Cada partida ocupa uma linha. Quando [mode] e informado, o
     * ranking considera apenas partidas daquele modo (classico ou genocida).
     */
    fun findRanking(page: Int, size: Int, mode: GameMode?): List<RankEntry>

    /** Melhor tentativa do usuario no modo informado (ou nulo se nao houver). */
    fun findUserBestScore(userId: UserId, mode: GameMode?): RankEntry?

    /**
     * Quantidade de tentativas estritamente melhores que a entrada dada,
     * restrita ao [mode] informado. Usado para calcular a posicao individual
     * (position = count + 1).
     */
    fun countUsersBetterThan(entry: RankEntry, mode: GameMode?): Long

    /** Total de tentativas no ranking do modo informado. */
    fun countRankedEntries(mode: GameMode?): Long
}
