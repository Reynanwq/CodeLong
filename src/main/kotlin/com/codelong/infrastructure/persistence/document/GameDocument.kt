package com.codelong.infrastructure.persistence.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "games")
@CompoundIndex(name = "game_ranking_idx", def = "{'status': 1, 'score': -1, 'correctAnswers': -1}")
@CompoundIndex(name = "game_user_idx", def = "{'userId': 1, 'status': 1}")
data class GameDocument(
    @field:Id val id: String = "",
    val userId: String = "",
    val username: String = "",
    val status: String = "",
    val startedAt: Instant = Instant.EPOCH,
    val completedAt: Instant? = null,
    val currentQuestionIndex: Int = 0,
    val questions: List<GameQuestionDocument> = emptyList(),
    val answers: List<AnswerDocument> = emptyList(),
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    @field:Version var version: Long = 0
)

data class GameQuestionDocument(
    val id: String = "",
    val statement: String = "",
    val options: List<OptionDocument> = emptyList(),
    val correctOption: String = "",
    val explanation: String = "",
    val category: String = "",
    val difficulty: String = ""
)

data class AnswerDocument(
    val questionIndex: Int = 0,
    val questionId: String = "",
    val chosenOption: String = "",
    val correct: Boolean = false,
    val earnedPoints: Int = 0,
    val answeredAt: Instant = Instant.EPOCH
)

/**
 * Projecao da agregacao de ranking (melhor partida por usuario).
 */
data class RankEntryDocument(
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val totalTimeMillis: Long = 0,
    val achievedAt: Instant = Instant.EPOCH
)