package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.QuestionId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeleteQuestionUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: DeleteQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = DeleteQuestionUseCaseImpl(repository)
    }

    @Test
    fun `remove a pergunta existente`() {
        repository.save(Fixtures.question(id = "q-1"))

        useCase.delete(QuestionId("q-1"))

        assertNull(repository.findById(QuestionId("q-1")))
    }

    @Test
    fun `pergunta inexistente gera erro`() {
        val error = assertThrows<DomainException> { useCase.delete(QuestionId("nao-existe")) }

        assertEquals("QUESTION_NOT_FOUND", error.code)
        assertEquals("Question not found", error.message)
    }

    @Test
    fun `nao remove outras perguntas`() {
        repository.save(Fixtures.question(id = "q-1"))
        repository.save(Fixtures.question(id = "q-2"))

        useCase.delete(QuestionId("q-1"))

        assertNull(repository.findById(QuestionId("q-1")))
        assertEquals("q-2", repository.findById(QuestionId("q-2"))?.id?.value)
    }

    @Test
    fun `remover duas vezes gera erro na segunda`() {
        repository.save(Fixtures.question(id = "q-1"))

        useCase.delete(QuestionId("q-1"))

        assertThrows<DomainException> { useCase.delete(QuestionId("q-1")) }
    }

    @Test
    fun `remove pergunta inativa`() {
        repository.save(Fixtures.question(id = "q-1").deactivate(Fixtures.NOW))

        useCase.delete(QuestionId("q-1"))

        assertNull(repository.findById(QuestionId("q-1")))
    }

    @Test
    fun `nao afeta a contagem de ativas de outras perguntas`() {
        repository.save(Fixtures.question(id = "q-1"))
        repository.save(Fixtures.question(id = "q-2"))

        useCase.delete(QuestionId("q-1"))

        assertEquals(1L, repository.countActive())
    }
}
