package com.codelong.infrastructure.web.dto

import com.codelong.application.usecase.ThemeSummary
import com.codelong.domain.model.Question

/** Tema (categoria) com a quantidade de perguntas ativas. */
data class ThemeResponse(
    val category: String,
    val totalQuestions: Int
) {
    companion object {
        fun from(summary: ThemeSummary) = ThemeResponse(
            category = summary.category.name,
            totalQuestions = summary.totalQuestions
        )
    }
}

/** Pergunta de um tema, sem expor a resposta correta. */
data class LearningQuestionResponse(
    val id: String,
    val statement: String,
    val category: String,
    val difficulty: String
) {
    companion object {
        fun from(question: Question) = LearningQuestionResponse(
            id = question.idText,
            statement = question.statement,
            category = question.categoryName,
            difficulty = question.difficultyName
        )
    }
}

/** Envelope com os temas disponiveis. */
data class ThemesResponse(
    val themes: List<ThemeResponse>
)

/** Envelope com as perguntas de um tema. */
data class ThemeQuestionsResponse(
    val questions: List<LearningQuestionResponse>
)
