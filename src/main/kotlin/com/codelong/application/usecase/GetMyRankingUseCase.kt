package com.codelong.application.usecase

import com.codelong.domain.port.RankingFilter
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface GetMyRankingUseCase {
    fun myRanking(userId: UserId, filter: RankingFilter): RankEntry?
}


class GetMyRankingUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetMyRankingUseCase {


    /** Retorna a melhor entrada do usuario (no recorte informado) com a posicao, ou null. */
    override fun myRanking(userId: UserId, filter: RankingFilter): RankEntry? {
        val best = rankingRepository.findUserBestScore(userId, filter) ?: return null
        val position = (rankingRepository.countUsersBetterThan(best, filter) + 1).toInt()
        return best.copy(position = position)
    }
}