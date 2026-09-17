package com.codelong.application.command

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionContent

/**
 * Command que carrega o conteudo de uma pergunta. Implementado por criacao e
 * atualizacao, permitindo que os casos de uso montem o [QuestionContent] sem
 * espalhar a conversao.
 */
interface QuestionContentCommand {
    val statement: String
    val options: List<QuestionOptionCommand>
    val correctOption: String
    val explanation: String
    val category: Category
    val difficulty: Difficulty

    fun toContent(): QuestionContent = QuestionContent(
        statement = statement.trim(),
        options = options.map { it.toDomain() },
        correctOption = OptionId(correctOption.trim()),
        explanation = explanation.trim(),
        category = category,
        difficulty = difficulty
    )
}