package com.codelong.application.usecase

import com.codelong.domain.port.RankingRepository
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId

interface GetMyRankingUseCase {
    fun myRanking(userId: UserId, mode: GameMode?): RankEntry?
}


class GetMyRankingUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetMyRankingUseCase {


    /** Retorna a melhor entrada do usuario (no modo informado) com a posicao, ou null. */
    override fun myRanking(userId: UserId, mode: GameMode?): RankEntry? {
        val best = rankingRepository.findUserBestScore(userId, mode) ?: return null
        val position = (rankingRepository.countUsersBetterThan(best, mode) + 1).toInt()
        return best.copy(position = position)
    }
}