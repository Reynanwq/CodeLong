package com.codelong.application.usecase

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LearningUseCasesTest {

    private val repository = InMemoryQuestionRepository()
    private val themes = ListThemesUseCaseImpl(repository)
    private val questions = ListThemeQuestionsUseCaseImpl(repository)

    @Test
    fun `lista temas com a contagem de perguntas ativas`() {
        repository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))
        repository.save(Fixtures.question(id = "k-2", category = Category.KOTLIN))
        repository.save(Fixtures.question(id = "r-1", category = Category.REST))

        val result = themes.themes()

        assertEquals(listOf(Category.KOTLIN, Category.REST), result.map { it.category })
        assertEquals(listOf(2, 1), result.map { it.totalQuestions })
    }

    @Test
    fun `ignora perguntas inativas na contagem e na listagem`() {
        repository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))
        repository.save(
            Fixtures.question(id = "k-2", category = Category.KOTLIN).deactivate(Fixtures.NOW)
        )

        assertEquals(1, themes.themes().single { it.category == Category.KOTLIN }.totalQuestions)
        assertEquals(listOf("k-1"), questions.questions(Category.KOTLIN).map { it.idText })
    }

    @Test
    fun `lista perguntas do tema em dificuldade crescente`() {
        repository.save(
            Fixtures.question(id = "k-hard", category = Category.KOTLIN, difficulty = Difficulty.HARD)
        )
        repository.save(
            Fixtures.question(id = "k-easy", category = Category.KOTLIN, difficulty = Difficulty.EASY)
        )
        repository.save(Fixtures.question(id = "r-1", category = Category.REST))

        val result = questions.questions(Category.KOTLIN)

        assertEquals(listOf("k-easy", "k-hard"), result.map { it.idText })
    }

    @Test
    fun `nao lista gubee como tema`() {
        repository.save(Fixtures.question(id = "g-1", category = Category.GUBEE))
        repository.save(Fixtures.question(id = "k-1", category = Category.KOTLIN))

        assertEquals(listOf(Category.KOTLIN), themes.themes().map { it.category })
    }

    @Test
    fun `tema sem perguntas devolve lista vazia`() {
        assertEquals(emptyList<Category>(), themes.themes().map { it.category })
        assertEquals(emptyList<String>(), questions.questions(Category.KOTLIN).map { it.idText })
    }
}
