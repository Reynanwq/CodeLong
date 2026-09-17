package com.codelong.infrastructure.web.dto

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class QuestionDtosTest {

    private fun optionRequests() = listOf(
        OptionRequest("a", "Alternativa A"),
        OptionRequest("b", "Alternativa B")
    )

    private fun createRequest(
        statement: String = "Enunciado?",
        options: List<OptionRequest> = optionRequests(),
        correctOption: String = "a",
        explanation: String = "Explicacao.",
        category: String = "KOTLIN",
        difficulty: String = "EASY"
    ) = CreateQuestionRequest(statement, options, correctOption, explanation, category, difficulty)

    private fun updateRequest(
        statement: String = "Enunciado?",
        options: List<OptionRequest> = optionRequests(),
        correctOption: String = "a",
        explanation: String = "Explicacao.",
        category: String = "KOTLIN",
        difficulty: String = "EASY"
    ) = UpdateQuestionRequest(statement, options, correctOption, explanation, category, difficulty)

    @Test
    fun `CreateQuestionRequest expoe os campos recebidos`() {
        val request = createRequest()

        assertEquals("Enunciado?", request.statement)
        assertEquals(2, request.options.size)
        assertEquals("a", request.correctOption)
        assertEquals("Explicacao.", request.explanation)
        assertEquals("KOTLIN", request.category)
        assertEquals("EASY", request.difficulty)
    }

    @Test
    fun `UpdateQuestionRequest expoe os campos recebidos`() {
        val request = updateRequest()

        assertEquals("Enunciado?", request.statement)
        assertEquals(2, request.options.size)
        assertEquals("a", request.correctOption)
        assertEquals("Explicacao.", request.explanation)
        assertEquals("KOTLIN", request.category)
        assertEquals("EASY", request.difficulty)
    }

    @Test
    fun `CreateQuestionRequest converte para command`() {
        val command = createRequest().toCommand()

        assertEquals("Enunciado?", command.statement)
        assertEquals(2, command.options.size)
        assertEquals("a", command.correctOption)
        assertEquals("Explicacao.", command.explanation)
        assertEquals(Category.KOTLIN, command.category)
        assertEquals(Difficulty.EASY, command.difficulty)
    }

    @Test
    fun `CreateQuestionRequest converte as opcoes preservando id e texto`() {
        val command = createRequest().toCommand()

        assertEquals("a", command.options.first().id)
        assertEquals("Alternativa A", command.options.first().text)
        assertEquals("b", command.options.last().id)
    }

    @Test
    fun `UpdateQuestionRequest converte para command com o identificador`() {
        val command = updateRequest(statement = "Atualizado?").toCommand(QuestionId("q-1"))

        assertEquals(QuestionId("q-1"), command.questionId)
        assertEquals("Atualizado?", command.statement)
        assertEquals(Category.KOTLIN, command.category)
        assertEquals(Difficulty.EASY, command.difficulty)
    }

    @ParameterizedTest
    @CsvSource(
        "EASY,EASY",
        "easy,EASY",
        "Easy,EASY",
        "MASTER,MASTER",
        "master,MASTER",
        "very_easy,VERY_EASY",
        "VERY_HARD,VERY_HARD",
        "hard_plus,HARD_PLUS",
        "medium_plus,MEDIUM_PLUS",
        "expert,EXPERT"
    )
    fun `difficulty e normalizado para o enum`(raw: String, expected: Difficulty) {
        assertEquals(expected, createRequest(difficulty = raw).toCommand().difficulty)
        assertEquals(expected, updateRequest(difficulty = raw).toCommand(QuestionId("q-1")).difficulty)
    }

    @ParameterizedTest
    @ValueSource(strings = ["INVALIDO", "", " ", "nivel-1", "10", "FACIL", "unknown"])
    fun `difficulty invalido gera erro`(raw: String) {
        val error = assertThrows<DomainException> { createRequest(difficulty = raw).toCommand() }

        assertEquals("difficulty.invalid", error.code)
    }

    @Test
    fun `difficulty com espacos e aceito`() {
        assertEquals(Difficulty.HARD, createRequest(difficulty = "  hard  ").toCommand().difficulty)
    }

    @Test
    fun `categoria invalida gera erro`() {
        val error = assertThrows<DomainException> { createRequest(category = "JAVASCRIPT").toCommand() }

        assertEquals("category.invalid", error.code)
    }

    @Test
    fun `categoria em minusculo e aceita`() {
        assertEquals(Category.SPRING, createRequest(category = "spring").toCommand().category)
    }

    @Test
    fun `QuestionResponse converte a pergunta`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.MEDIUM, category = Category.TESTING)

        val response = QuestionResponse.from(question)

        assertEquals("q-1", response.id)
        assertEquals(question.statement, response.statement)
        assertEquals(3, response.options.size)
        assertEquals(question.correctOption.value, response.correctOption)
        assertEquals(question.explanation, response.explanation)
        assertEquals("TESTING", response.category)
        assertEquals("MEDIUM", response.difficulty)
        assertEquals("ACTIVE", response.status)
        assertEquals(question.createdAt, response.createdAt)
        assertEquals(question.updatedAt, response.updatedAt)
    }

    @Test
    fun `QuestionResponse converte pergunta inativa`() {
        val question = Fixtures.question(id = "q-1").deactivate(Fixtures.NOW)

        assertEquals("INACTIVE", QuestionResponse.from(question).status)
    }

    @Test
    fun `OptionResponse converte a alternativa`() {
        val option = com.codelong.domain.valueobject.QuestionOption(OptionId("opt-0"), "Texto")

        val response = OptionResponse.from(option)

        assertEquals("opt-0", response.id)
        assertEquals("Texto", response.text)
    }

    @Test
    fun `ChangeQuestionStatusRequest carrega a flag active`() {
        assertEquals(true, ChangeQuestionStatusRequest(active = true).active)
        assertEquals(false, ChangeQuestionStatusRequest(active = false).active)
    }
}
