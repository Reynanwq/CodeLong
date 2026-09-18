package com.codelong.infrastructure.persistence.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = MongoSchema.Collection.GAMES)
@CompoundIndex(name = MongoSchema.Index.GAME_RANKING, def = MongoSchema.Index.GAME_RANKING_DEF)
@CompoundIndex(name = MongoSchema.Index.GAME_USER, def = MongoSchema.Index.GAME_USER_DEF)
data class GameDocument(
    @field:Id val id: String = "",
    val userId: String = "",
    val username: String = "",
    val status: String = "",
    val mode: String = "",
    val startedAt: Instant = Instant.EPOCH,
    val completedAt: Instant? = null,
    val currentQuestionIndex: Int = 0,
    val currentQuestionDeadline: Instant = Instant.EPOCH,
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
    val chosenOption: String? = null,
    val correct: Boolean = false,
    val earnedPoints: Int = 0,
    val answeredAt: Instant = Instant.EPOCH,
    val timedOut: Boolean = false
)

/**
 * Projecao da agregacao de ranking (melhor partida por usuario).
 */
data class RankEntryDocument(
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val answeredQuestions: Int = 0,
    val totalTimeMillis: Long = 0,
    val achievedAt: Instant = Instant.EPOCH
)
