package com.codelong.domain.port

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface RankingRepository {
    /**
     * Ranking paginado com **todas as tentativas** elegiveis (concluidas,
     * abandonadas ou derrotadas), ordenado pela politica de desempate do
     * dominio. Cada partida ocupa uma linha, independentemente do modo.
     */
    fun findRanking(page: Int, size: Int): List<RankEntry>

    /** Melhor tentativa do usuario (ou nulo se ele nao tiver nenhuma). */
    fun findUserBestScore(userId: UserId): RankEntry?

    /**
     * Quantidade de tentativas estritamente melhores que a entrada dada.
     * Usado para calcular a posicao individual (position = count + 1).
     */
    fun countUsersBetterThan(entry: RankEntry): Long

    /** Total de tentativas no ranking. */
    fun countRankedEntries(): Long
}
