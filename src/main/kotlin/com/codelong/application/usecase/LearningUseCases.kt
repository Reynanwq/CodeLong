package com.codelong.application.usecase

import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.Category

/** Resumo de um tema (categoria) exibido na tela de Aprendizado. */
data class ThemeSummary(val category: Category, val totalQuestions: Int)

/** Lista os temas disponiveis (categorias com perguntas ativas). */
interface ListThemesUseCase {
    fun themes(): List<ThemeSummary>
}

class ListThemesUseCaseImpl(
    private val questionRepository: QuestionRepository
) : ListThemesUseCase {

    override fun themes(): List<ThemeSummary> =
        questionRepository.findAllActive()
            .filterNot { it.category == Category.GUBEE }
            .groupingBy { it.category }
            .eachCount()
            .map { (category, count) -> ThemeSummary(category, count) }
            .sortedBy { it.category.name }
}

/** Lista as perguntas ativas de um tema, em dificuldade crescente. */
interface ListThemeQuestionsUseCase {
    fun questions(category: Category): List<Question>
}

class ListThemeQuestionsUseCaseImpl(
    private val questionRepository: QuestionRepository
) : ListThemeQuestionsUseCase {

    override fun questions(category: Category): List<Question> =
        questionRepository.findActiveByCategory(category)
            .sortedWith(compareBy({ it.difficulty.level }, { it.statement }))
}
