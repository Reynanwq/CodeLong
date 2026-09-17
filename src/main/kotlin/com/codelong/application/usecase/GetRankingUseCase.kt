package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.result.RankingPage
import com.codelong.domain.port.RankingRepository

class GetRankingUseCase(
    private val rankingRepository: RankingRepository
) {

    fun ranking(page: Int, size: Int): RankingPage {
        (page < 0).takeIf { it }?.let {
            throw DomainException.invalidInput("pagination.page.invalid", "Page must be greater than or equal to 0")
        }
        (size !in MIN_SIZE..MAX_SIZE).takeIf { it }?.let {
            throw DomainException.invalidInput(
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