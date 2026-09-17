package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Estado completo de um [com.codelong.domain.model.Question] reconstruido pela
 * persistencia ou por testes.
 */
data class QuestionState(
    val id: QuestionId,
    val content: QuestionContent,
    val status: QuestionStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val idText: String get() = id.value

    val statementText: String get() = content.statementText

    val options: List<QuestionOption> get() = content.options

    val correctOptionText: String get() = content.correctOptionText

    val explanationText: String get() = content.explanationText

    val categoryName: String get() = content.categoryName

    val difficultyName: String get() = content.difficultyName

    val statusName: String get() = status.name
}