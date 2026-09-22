package com.codelong.infrastructure.web.dto

import com.codelong.application.usecase.ThemeSummary
import com.codelong.domain.valueobject.Category
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LearningDtosTest {

    @Test
    fun `ThemeResponse mapeia o resumo do tema`() {
        val response = ThemeResponse.from(ThemeSummary(Category.KOTLIN, 42))

        assertEquals("KOTLIN", response.category)
        assertEquals(42, response.totalQuestions)
    }

    @Test
    fun `envelopes carregam os itens`() {
        val theme = ThemeResponse.from(ThemeSummary(Category.KOTLIN, 1))
        val question = LearningQuestionResponse.from(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        assertEquals(listOf(theme), ThemesResponse(listOf(theme)).themes)
        assertEquals(listOf(question), ThemeQuestionsResponse(listOf(question)).questions)
    }

    @Test
    fun `LearningQuestionResponse nao expoe a resposta correta`() {
        val question = Fixtures.question(id = "k-1", category = Category.KOTLIN)

        val response = LearningQuestionResponse.from(question)

        assertEquals("k-1", response.id)
        assertEquals("KOTLIN", response.category)
        assertEquals("EASY", response.difficulty)
        assertEquals(question.statement, response.statement)
    }
}
