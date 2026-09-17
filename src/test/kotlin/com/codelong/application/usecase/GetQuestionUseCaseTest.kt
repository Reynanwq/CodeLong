package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetQuestionUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: GetQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = GetQuestionUseCaseImpl(repository)
    }

    @Test
    fun `devolve a pergunta existente`() {
        repository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.HARD))

        val question = useCase.get(QuestionId("q-1"))

        assertEquals("q-1", question.id.value)
        assertEquals(Difficulty.HARD, question.difficulty())
    }

    @Test
    fun `devolve a instancia armazenada`() {
        repository.save(Fixtures.question(id = "q-1"))

        assertSame(repository.findById(QuestionId("q-1")), useCase.get(QuestionId("q-1")))
    }

    @Test
    fun `devolve pergunta inativa para consulta administrativa`() {
        repository.save(Fixtures.question(id = "q-1").deactivate(Fixtures.NOW))

        assertEquals(false, useCase.get(QuestionId("q-1")).isActive())
    }

    @Test
    fun `pergunta inexistente gera erro`() {
        val error = assertThrows<DomainException> { useCase.get(QuestionId("nao-existe")) }

        assertEquals("QUESTION_NOT_FOUND", error.code)
        assertEquals("Question not found", error.message)
    }

    @Test
    fun `nao devolve pergunta de outro identificador`() {
        repository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.get(QuestionId("q-2")) }
    }
}
