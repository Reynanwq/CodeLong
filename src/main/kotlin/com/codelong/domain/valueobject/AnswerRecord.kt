package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Registro de uma resposta dada a uma pergunta dentro de uma partida.
 *
 * [chosenOption] e nulo quando o tempo para responder expirou: nesse caso a
 * pergunta e contabilizada como erro ([timedOut]).
 */
data class AnswerRecord(
    val questionIndex: Int,
    val questionId: QuestionId,
    val chosenOption: OptionId?,
    val correct: Boolean,
    val earnedPoints: Int,
    val answeredAt: Instant,
    val timedOut: Boolean = false
) {
    val questionIdText: String get() = questionId.value

    val chosenOptionText: String? get() = chosenOption?.value
}
