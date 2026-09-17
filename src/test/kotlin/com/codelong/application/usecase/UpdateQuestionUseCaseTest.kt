package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.QuestionOptionCommand
import com.codelong.application.command.UpdateQuestionCommand
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UpdateQuestionUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: UpdateQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = UpdateQuestionUseCaseImpl(repository, TestClock.fixed)
    }

    private fun command(
        questionId: String = "q-1",
        statement: String = "Enunciado atualizado?",
        correctOption: String = "a",
        category: Category = Category.KOTLIN,
        difficulty: Difficulty = Difficulty.MASTER
    ) = UpdateQuestionCommand(
        questionId = QuestionId(questionId),
        statement = statement,
        options = listOf(
            QuestionOptionCommand("a", "Alternativa A"),
            QuestionOptionCommand("b", "Alternativa B")
        ),
        correctOption = correctOption,
        explanation = "Explicacao atualizada.",
        category = category,
        difficulty = difficulty
    )

    @Test
    fun `atualiza o conteudo da pergunta`() {
        repository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))

        val updated = useCase.update(command())

        assertEquals("Enunciado atualizado?", updated.statement)
        assertEquals(2, updated.options.size)
        assertEquals(OptionId("a"), updated.correctOption)
        assertEquals("Explicacao atualizada.", updated.explanation)
        assertEquals(Category.KOTLIN, updated.category)
        assertEquals(Difficulty.MASTER, updated.difficulty)
    }

    @Test
    fun `atualiza a data de modificacao`() {
        repository.save(Fixtures.question(id = "q-1"))

        val updated = useCase.update(command())

        assertEquals(Fixtures.NOW, updated.updatedAt)
        assertEquals(Fixtures.NOW, updated.createdAt)
    }

    @Test
    fun `persiste a alteracao no repositorio`() {
        repository.save(Fixtures.question(id = "q-1"))

        useCase.update(command(statement = "Persistido?"))

        val stored = repository.findById(QuestionId("q-1"))
        assertNotNull(stored)
        assertEquals("Persistido?", stored!!.statement)
        assertEquals(Difficulty.MASTER, stored.difficulty)
    }

    @Test
    fun `preserva o status inativo`() {
        repository.save(Fixtures.question(id = "q-1").deactivate(Fixtures.NOW))

        val updated = useCase.update(command())

        assertEquals(QuestionStatus.INACTIVE, updated.status)
    }

    @Test
    fun `preserva o status ativo`() {
        repository.save(Fixtures.question(id = "q-1"))

        assertEquals(QuestionStatus.ACTIVE, useCase.update(command()).status)
    }

    @Test
    fun `pergunta inexistente gera erro`() {
        val error = assertThrows<DomainException> { useCase.update(command(questionId = "nao-existe")) }

        assertEquals("QUESTION_NOT_FOUND", error.code)
        assertEquals("Question not found", error.message)
    }

    @Test
    fun `conteudo invalido nao atualiza a pergunta`() {
        repository.save(Fixtures.question(id = "q-1"))

        assertThrows<DomainException> { useCase.update(command(statement = "   ")) }

        assertEquals("O que e polimorfismo?", repository.findById(QuestionId("q-1"))!!.statement)
    }

    @Test
    fun `alternativa correta inexistente gera erro`() {
        repository.save(Fixtures.question(id = "q-1"))

        val error = assertThrows<DomainException> { useCase.update(command(correctOption = "z")) }

        assertEquals("question.correctOption.invalid", error.code)
    }

    @Test
    fun `remove espacos do enunciado e das alternativas`() {
        repository.save(Fixtures.question(id = "q-1"))

        val updated = useCase.update(command(statement = "  Com espacos  "))

        assertEquals("Com espacos", updated.statement)
        assertEquals("Alternativa A", updated.options.first().text)
    }
}
