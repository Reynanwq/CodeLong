package com.codelong.domain.valueobject

import java.time.Instant

/**
 * Resultado do processamento de uma resposta dentro do dominio.
 * O cliente jamais informa ou recebe o resultado antes do calculo.
 */
data class AnswerEval(
    val record: AnswerRecord,
    val question: GameQuestion,
    val currentScore: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val gameCompleted: Boolean,
    val questionIndex: Int,
    val totalQuestions: Int
)

data class AnswerResult(
    val record: AnswerRecord,
    val question: GameQuestion,
    val currentScore: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val gameCompleted: Boolean,
    val questionIndex: Int,
    val totalQuestions: Int,
    val nextQuestion: QuestionPublic?,
    val nextQuestionDeadline: Instant?
)