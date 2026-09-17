package com.codelong.application.usecase

import com.codelong.application.command.QuestionSearchQuery
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListQuestionsUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: ListQuestionsUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = ListQuestionsUseCaseImpl(repository)

        repository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY, category = Category.OOP))
        repository.save(Fixtures.question(id = "q-2", difficulty = Difficulty.HARD, category = Category.KOTLIN))
        repository.save(
            Fixtures.question(id = "q-3", difficulty = Difficulty.EASY, category = Category.OOP)
                .deactivate(Fixtures.NOW)
        )
    }

    @Test
    fun `sem filtros devolve todas as perguntas`() {
        val page = useCase.list(QuestionSearchQuery())

        assertEquals(3L, page.totalElements)
        assertEquals(3, page.items.size)
    }

    @Test
    fun `filtra por status`() {
        val active = useCase.list(QuestionSearchQuery(status = QuestionStatus.ACTIVE))
        val inactive = useCase.list(QuestionSearchQuery(status = QuestionStatus.INACTIVE))

        assertEquals(2L, active.totalElements)
        assertEquals(1L, inactive.totalElements)
        assertEquals("q-3", inactive.items.first().id.value)
    }

    @Test
    fun `filtra por categoria`() {
        val page = useCase.list(QuestionSearchQuery(category = Category.OOP))

        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `filtra por dificuldade`() {
        val page = useCase.list(QuestionSearchQuery(difficulty = Difficulty.HARD))

        assertEquals(1L, page.totalElements)
        assertEquals("q-2", page.items.first().id.value)
    }

    @Test
    fun `combina filtros`() {
        val page = useCase.list(
            QuestionSearchQuery(
                status = QuestionStatus.ACTIVE,
                category = Category.OOP,
                difficulty = Difficulty.EASY
            )
        )

        assertEquals(1L, page.totalElements)
        assertEquals("q-1", page.items.first().id.value)
    }

    @Test
    fun `filtro sem resultados devolve pagina vazia`() {
        val page = useCase.list(QuestionSearchQuery(category = Category.KAFKA))

        assertEquals(0L, page.totalElements)
        assertEquals(0, page.items.size)
    }

    @Test
    fun `pagina os resultados`() {
        val first = useCase.list(QuestionSearchQuery(page = 0, size = 2))
        val second = useCase.list(QuestionSearchQuery(page = 1, size = 2))

        assertEquals(2, first.items.size)
        assertEquals(1, second.items.size)
        assertEquals(3L, first.totalElements)
        assertEquals(3L, second.totalElements)
    }

    @Test
    fun `preserva pagina e tamanho na resposta`() {
        val page = useCase.list(QuestionSearchQuery(page = 2, size = 7))

        assertEquals(2, page.page)
        assertEquals(7, page.size)
    }
}
