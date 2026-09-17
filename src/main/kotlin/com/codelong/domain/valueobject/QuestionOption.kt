package com.codelong.domain.valueobject

/**
 * Uma alternativa de resposta de uma pergunta de multipla escolha.
 */
data class QuestionOption(
    val id: OptionId,
    val text: String
)