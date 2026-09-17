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
    fun idText(): String = id.value

    fun statementText(): String = content.statementText()

    fun options(): List<QuestionOption> = content.options

    fun correctOptionText(): String = content.correctOptionText()

    fun explanationText(): String = content.explanationText()

    fun categoryName(): String = content.categoryName()

    fun difficultyName(): String = content.difficultyName()

    fun statusName(): String = status.name
}