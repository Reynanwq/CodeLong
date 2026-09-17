package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors



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

    fun statementText(): String = statement

    fun explanationText(): String = explanation

    fun correctOptionText(): String = correctOption.value

    fun categoryName(): String = category.name

    fun difficultyName(): String = difficulty.name

    private fun validate() {
        when {
            statement.isBlank() || statement.length > MAX_STATEMENT -> throw Errors.statementInvalid(MAX_STATEMENT)

            options.size < MIN_OPTIONS -> throw Errors.tooFewOptions(MIN_OPTIONS)

            options.any { it.text.isBlank() } -> throw Errors.blankOptionText()

            options.map { it.id }.distinct().size != options.size -> throw Errors.duplicatedOptionIds()

            !hasOption(correctOption) -> throw Errors.correctOptionNotInOptions()

            explanation.isBlank() || explanation.length > MAX_EXPLANATION ->
                throw Errors.explanationInvalid(MAX_EXPLANATION)
        }
    }

    private companion object {
        const val MIN_OPTIONS = 2
        const val MAX_STATEMENT = 500
        const val MAX_EXPLANATION = 1000
    }
}