package com.codelong.application.usecase

import com.codelong.domain.port.RankingRepository
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface GetMyRankingUseCase {
    fun myRanking(userId: UserId): RankEntry?
}


class GetMyRankingUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetMyRankingUseCase {


    /** Retorna a melhor entrada do usuario com a posicao, ou null se nunca concluiu uma partida. */
    override fun myRanking(userId: UserId): RankEntry? {
        val best = rankingRepository.findUserBestScore(userId) ?: return null
        val position = (rankingRepository.countUsersBetterThan(best) + 1).toInt()
        return best.copy(position = position)
    }
}