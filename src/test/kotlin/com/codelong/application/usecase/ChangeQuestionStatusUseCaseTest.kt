package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangeQuestionStatusCommand
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChangeQuestionStatusUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: ChangeQuestionStatusUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = ChangeQuestionStatusUseCaseImpl(repository, TestClock.fixed)
    }

    @Test
    fun `desativa a pergunta`() {
        repository.save(Fixtures.question(id = "q-1"))

        val question = useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))

        assertEquals(QuestionStatus.INACTIVE, question.status())
        assertFalse(question.isActive())
    }

    @Test
    fun `ativa a pergunta`() {
        repository.save(Fixtures.question(id = "q-1").deactivate(Fixtures.NOW))

        val question = useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = true))

        assertEquals(QuestionStatus.ACTIVE, question.status())
        assertTrue(question.isActive())
    }

    @Test
    fun `atualiza a data de modificacao`() {
        repository.save(Fixtures.question(id = "q-1"))

        val question = useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))

        assertEquals(Fixtures.NOW, question.updatedAt())
        assertEquals(Fixtures.NOW, question.createdAt)
    }

    @Test
    fun `persiste a mudanca de status`() {
        repository.save(Fixtures.question(id = "q-1"))

        useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))

        assertEquals(QuestionStatus.INACTIVE, repository.findById(QuestionId("q-1"))!!.status())
        assertEquals(0L, repository.countActive())
    }

    @Test
    fun `pergunta inexistente gera erro`() {
        val error = assertThrows<DomainException> {
            useCase.change(ChangeQuestionStatusCommand(QuestionId("nao-existe"), active = true))
        }

        assertEquals("QUESTION_NOT_FOUND", error.code)
    }

    @Test
    fun `desativar duas vezes e permitido`() {
        repository.save(Fixtures.question(id = "q-1"))

        useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))
        val question = useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))

        assertEquals(QuestionStatus.INACTIVE, question.status())
    }

    @Test
    fun `reativar pergunta ja ativa e permitido`() {
        repository.save(Fixtures.question(id = "q-1"))

        val question = useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = true))

        assertEquals(QuestionStatus.ACTIVE, question.status())
    }

    @Test
    fun `nao altera outras perguntas`() {
        repository.save(Fixtures.question(id = "q-1"))
        repository.save(Fixtures.question(id = "q-2"))

        useCase.change(ChangeQuestionStatusCommand(QuestionId("q-1"), active = false))

        assertEquals(QuestionStatus.ACTIVE, repository.findById(QuestionId("q-2"))!!.status())
    }
}
