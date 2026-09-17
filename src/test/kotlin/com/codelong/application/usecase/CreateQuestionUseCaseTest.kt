package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.CreateQuestionCommand
import com.codelong.application.command.QuestionOptionCommand
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.support.InMemoryQuestionRepository
import com.codelong.support.TestClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CreateQuestionUseCaseTest {

    private lateinit var repository: InMemoryQuestionRepository
    private lateinit var useCase: CreateQuestionUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryQuestionRepository()
        useCase = CreateQuestionUseCaseImpl(repository, TestClock.fixed)
    }

    private fun command(
        statement: String = "O que e uma interface?",
        difficulty: Difficulty = Difficulty.EASY,
        category: Category = Category.OOP,
        correctOption: String = "a"
    ) = CreateQuestionCommand(
        statement = statement,
        options = listOf(
            QuestionOptionCommand("a", "Contrato sem implementacao"),
            QuestionOptionCommand("b", "Classe concreta")
        ),
        correctOption = correctOption,
        explanation = "Interfaces definem contratos.",
        category = category,
        difficulty = difficulty
    )

    @Test
    fun `cria pergunta ativa`() {
        val question = useCase.create(command())

        assertEquals(QuestionStatus.ACTIVE, question.status)
        assertTrue(question.isActive)
    }

    @Test
    fun `cria pergunta com os dados informados`() {
        val question = useCase.create(command(statement = "Enunciado?", category = Category.SOLID))

        assertEquals("Enunciado?", question.statement)
        assertEquals(Category.SOLID, question.category)
        assertEquals(OptionId("a"), question.correctOption)
        assertEquals(2, question.options.size)
    }

    @Test
    fun `usa o instante do clock`() {
        val question = useCase.create(command())

        assertEquals(com.codelong.support.Fixtures.NOW, question.createdAt)
        assertEquals(com.codelong.support.Fixtures.NOW, question.updatedAt)
    }

    @Test
    fun `persiste a pergunta criada`() {
        val question = useCase.create(command())

        assertEquals(question.id, repository.findById(question.id)?.id)
    }

    @Test
    fun `gera identificadores distintos`() {
        val ids = (1..100).map { useCase.create(command()).id.value }

        assertEquals(100, ids.distinct().size)
    }

    @Test
    fun `remove espacos do enunciado e da explicacao`() {
        val question = useCase.create(command(statement = "  Com espacos  "))

        assertEquals("Com espacos", question.statement)
        assertEquals("Interfaces definem contratos.", question.explanation)
    }

    @Test
    fun `conteudo invalido nao cria pergunta`() {
        assertThrows<DomainException> { useCase.create(command(statement = "   ")) }

        assertEquals(0, repository.countActive())
    }

    @Test
    fun `alternativa correta invalida nao cria pergunta`() {
        val error = assertThrows<DomainException> { useCase.create(command(correctOption = "z")) }

        assertEquals("question.correctOption.invalid", error.code)
    }

    @ParameterizedTest
    @EnumSource(Difficulty::class)
    fun `aceita todos os niveis de dificuldade`(difficulty: Difficulty) {
        val question = useCase.create(command(difficulty = difficulty))

        assertEquals(difficulty, question.difficulty)
    }

    @ParameterizedTest
    @EnumSource(Category::class)
    fun `aceita todas as categorias`(category: Category) {
        val question = useCase.create(command(category = category))

        assertEquals(category, question.category)
    }

    @Test
    fun `pergunta criada entra na contagem de ativas`() {
        useCase.create(command())

        assertEquals(1L, repository.countActive())
        assertEquals(1, repository.findAllActive().size)
    }
}
