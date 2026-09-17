package com.codelong.infrastructure.web.dto

import com.codelong.application.result.RankingPage
import com.codelong.domain.valueobject.RankEntry
import java.time.Instant

data class RankingEntryResponse(
    val position: Int?,
    val userId: String,
    val username: String,
    val score: Int,
    val correctAnswers: Int,
    val totalTimeMillis: Long,
    val achievedAt: Instant
) {
    companion object {
        fun from(entry: RankEntry) = RankingEntryResponse(
            position = entry.position,
            userId = entry.userId.value,
            username = entry.username,
            score = entry.score,
            correctAnswers = entry.correctAnswers,
            totalTimeMillis = entry.totalTimeMillis,
            achievedAt = entry.achievedAt
        )
    }
}

data class RankingResponse(
    val entries: List<RankingEntryResponse>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        fun from(result: RankingPage): RankingResponse {
            val totalPages = result.size
                .takeIf { it > 0 }
                ?.let { ((result.totalElements + it - 1) / it).toInt() }
                ?: 0
            return RankingResponse(
                entries = result.entries.map(RankingEntryResponse::from),
                totalElements = result.totalElements,
                page = result.page,
                size = result.size,
                totalPages = totalPages
            )
        }
    }
}