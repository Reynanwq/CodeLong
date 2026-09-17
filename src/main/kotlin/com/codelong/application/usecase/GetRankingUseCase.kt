package com.codelong.application.usecase

import com.codelong.application.result.RankingPage
import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.port.RankingRepository

class GetRankingUseCase(
    private val rankingRepository: RankingRepository
) {

    fun ranking(page: Int, size: Int): RankingPage {
        if (page < 0) {
            throw InvalidInputException("pagination.page.invalid", "Page must be greater than or equal to 0")
        }
        if (size !in MIN_SIZE..MAX_SIZE) {
            throw InvalidInputException(
                "pagination.size.invalid",
                "Page size must be between $MIN_SIZE and $MAX_SIZE"
            )
        }

        val entries = rankingRepository.findRanking(page, size)
        val positioned = entries.mapIndexed { index, entry ->
            entry.copy(position = page * size + index + 1)
        }

        return RankingPage(
            entries = positioned,
            totalElements = rankingRepository.countRankedUsers(),
            page = page,
            size = size
        )
    }

    private companion object {
        const val MIN_SIZE = 1
        const val MAX_SIZE = 100
    }
}