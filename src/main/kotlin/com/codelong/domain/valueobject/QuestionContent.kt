package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException


/**
 * Conteudo imutavel de uma pergunta. Agrupa os dados que definem uma pergunta
 * e protege as invariantes do agregado (>= 2 opcoes, resposta correta entre as
 * opcoes, ids de opcao unicos, textos obrigatorios).
 */
data class QuestionContent(
    val statement: String,
    val options: List<QuestionOption>,
    val correctOption: OptionId,
    val explanation: String,
    val category: Category,
    val difficulty: Difficulty
) {

    init {
        validate()
    }

    fun hasOption(optionId: OptionId): Boolean = options.any { it.id == optionId }

    fun isCorrect(optionId: OptionId): Boolean = correctOption == optionId

    private fun validate() {
        if (statement.isBlank() || statement.length > MAX_STATEMENT) {
            throw DomainException.invalidInput(
                "question.statement.invalid",
                "Statement must not be blank and at most $MAX_STATEMENT characters"
            )
        }
        if (options.size < MIN_OPTIONS) {
            throw DomainException.invalidInput(
                "question.options.invalid",
                "A question must have at least $MIN_OPTIONS options"
            )
        }
        if (options.any { it.text.isBlank() }) {
            throw DomainException.invalidInput("question.options.invalid", "Option text must not be blank")
        }
        if (options.map { it.id }.distinct().size != options.size) {
            throw DomainException.invalidInput("question.options.invalid", "Option ids must be unique")
        }
        if (!hasOption(correctOption)) {
            throw DomainException.invalidInput(
                "question.correctOption.invalid",
                "The correct option must be one of the options"
            )
        }
        if (explanation.isBlank() || explanation.length > MAX_EXPLANATION) {
            throw DomainException.invalidInput(
                "question.explanation.invalid",
                "Explanation must not be blank and at most $MAX_EXPLANATION characters"
            )
        }
    }

    private companion object {
        const val MIN_OPTIONS = 2
        const val MAX_STATEMENT = 500
        const val MAX_EXPLANATION = 1000
    }
}