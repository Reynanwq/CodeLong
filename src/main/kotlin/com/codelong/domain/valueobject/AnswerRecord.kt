package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Registro de uma resposta dada a uma pergunta dentro de uma partida.
 */
data class AnswerRecord(
    val questionIndex: Int,
    val questionId: QuestionId,
    val chosenOption: OptionId,
    val correct: Boolean,
    val earnedPoints: Int,
    val answeredAt: Instant
) {
    fun questionIdText(): String = questionId.value

    fun chosenOptionText(): String = chosenOption.value
}