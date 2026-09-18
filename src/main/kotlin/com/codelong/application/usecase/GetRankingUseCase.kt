package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.result.RankingPage
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.valueobject.GameMode

interface GetRankingUseCase {
    fun ranking(page: Int, size: Int, mode: GameMode?): RankingPage
}


class GetRankingUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetRankingUseCase {


    override fun ranking(page: Int, size: Int, mode: GameMode?): RankingPage {
        (page < 0).takeIf { it }?.let {
            throw Errors.invalidPage()
        }
        (size !in MIN_SIZE..MAX_SIZE).takeIf { it }?.let {
            throw Errors.invalidPageSize(MIN_SIZE, MAX_SIZE)
        }

        val entries = rankingRepository.findRanking(page, size, mode)
        val positioned = entries.mapIndexed { index, entry ->
            entry.copy(position = page * size + index + 1)
        }

        return RankingPage(
            entries = positioned,
            totalElements = rankingRepository.countRankedEntries(mode),
            page = page,
            size = size
        )
    }

    private companion object {
        const val MIN_SIZE = 1
        const val MAX_SIZE = 100
    }
}