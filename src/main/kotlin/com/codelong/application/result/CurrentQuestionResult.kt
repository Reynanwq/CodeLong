package com.codelong.application.result

import com.codelong.domain.valueobject.QuestionPublic
import java.time.Instant

/**
 * Pergunta atual de uma partida, com a posicao na sequencia e o prazo para
 * responder.
 */
data class CurrentQuestionResult(
    val question: QuestionPublic,
    val index: Int,
    val total: Int,
    val deadline: Instant
)
