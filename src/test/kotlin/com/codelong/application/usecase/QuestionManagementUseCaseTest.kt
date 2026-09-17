package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangeQuestionStatusCommand
import com.codelong.application.command.CreateQuestionCommand
import com.codelong.application.command.QuestionOptionCommand
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuestionManagementUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var createUseCase: CreateQuestionUseCase
    private lateinit var changeStatusUseCase: ChangeQuestionStatusUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        createUseCase = CreateQuestionUseCaseImpl(repository, TestClock.fixed)
        changeStatusUseCase = ChangeQuestionStatusUseCaseImpl(repository, TestClock.fixed)
    }

    private fun command(
        correctOption: String = "a",
        options: List<QuestionOptionCommand> = listOf(
            QuestionOptionCommand("a", "Alternativa A"),
            QuestionOptionCommand("b", "Alternativa B")
        )
    ) = CreateQuestionCommand(
        statement = "O que e inversao de dependencia?",
        options = options,
        correctOption = correctOption,
        explanation = "Dependa de abstracoes, nao de implementacoes.",
        category = Category.SOLID,
        difficulty = Difficulty.EASY_PLUS
    )

    @Test
    fun `cria pergunta ativa e persiste`() {
        val created = createUseCase.create(command())

        assertTrue(created.isActive())
        assertEquals(Category.SOLID, created.category())
        assertEquals(Difficulty.EASY_PLUS, created.difficulty())
        assertEquals(1L, repository.countActive())
    }

    @Test
    fun `rejeita pergunta sem opcoes suficientes`() {
        assertThrows<DomainException> {
            createUseCase.create(command(options = listOf(QuestionOptionCommand("a", "Unica"))))
        }
    }

    @Test
    fun `rejeita resposta correta fora das opcoes`() {
        assertThrows<DomainException> {
            createUseCase.create(command(correctOption = "z"))
        }
    }

    @Test
    fun `desativa e reativa uma pergunta`() {
        val created = createUseCase.create(command())

        val deactivated = changeStatusUseCase.change(ChangeQuestionStatusCommand(created.id, false))
        assertFalse(deactivated.isActive())
        assertEquals(0L, repository.countActive())

        val reactivated = changeStatusUseCase.change(ChangeQuestionStatusCommand(created.id, true))
        assertTrue(reactivated.isActive())
        assertEquals(1L, repository.countActive())
    }

    @Test
    fun `pergunta inexistente`() {
        val error = assertThrows<DomainException> {
            changeStatusUseCase.change(ChangeQuestionStatusCommand(QuestionId("nao-existe"), false))
        }

        assertEquals("QUESTION_NOT_FOUND", error.code)
    }
}