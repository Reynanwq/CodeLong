package com.codelong.domain.port

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface RankingRepository {
    /**
     * Ranking paginado com a melhor pontuacao de cada usuario,
     * ordenado pela politica de desempate do dominio.
     */
    fun findRanking(page: Int, size: Int): List<RankEntry>

    fun findUserBestScore(userId: UserId): RankEntry?

    /**
     * Quantidade de usuarios estritamente melhores que a entrada dada.
     * Usado para calcular a posicao individual (position = count + 1).
     */
    fun countUsersBetterThan(entry: RankEntry): Long

    fun countRankedUsers(): Long
}