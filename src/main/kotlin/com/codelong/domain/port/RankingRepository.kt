package com.codelong.domain.port

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

/**
 * Recorte do ranking: por modo e, no modo APRENDIZADO, por tema (categoria).
 *
 * - [mode] nulo significa ranking global, que considera apenas CLASSIC e
 *   GENOCIDA (Aprendizado aparece somente nos rankings por tema).
 * - [theme] nulo significa todos os temas.
 */
data class RankingFilter(
    val mode: GameMode?,
    val theme: Category?
)

interface RankingRepository {
    /**
     * Ranking paginado com **todas as tentativas** elegiveis (concluidas,
     * abandonadas ou derrotadas), ordenado pela politica de desempate do
     * dominio. Cada partida ocupa uma linha.
     */
    fun findRanking(page: Int, size: Int, filter: RankingFilter): List<RankEntry>

    /** Melhor tentativa do usuario no recorte informado (ou nulo se nao houver). */
    fun findUserBestScore(userId: UserId, filter: RankingFilter): RankEntry?

    /**
     * Quantidade de tentativas estritamente melhores que a entrada dada,
     * restrita ao recorte informado. Usado para calcular a posicao individual
     * (position = count + 1).
     */
    fun countUsersBetterThan(entry: RankEntry, filter: RankingFilter): Long

    /** Total de tentativas no ranking do recorte informado. */
    fun countRankedEntries(filter: RankingFilter): Long
}
