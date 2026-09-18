package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.RankEntryDocument

object RankingPersistenceMapper {

    fun toDomain(document: RankEntryDocument): RankEntry = RankEntry(
        userId = UserId(document.userId),
        username = document.username,
        score = document.score,
        correctAnswers = document.correctAnswers,
        wrongAnswers = document.wrongAnswers,
        answeredQuestions = document.answeredQuestions,
        mode = document.mode.takeIf { it.isNotBlank() }?.let(GameMode::fromName) ?: GameMode.CLASSIC,
        totalTimeMillis = document.totalTimeMillis,
        achievedAt = document.achievedAt
    )
}
