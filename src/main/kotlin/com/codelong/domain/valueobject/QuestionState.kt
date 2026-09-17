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
)