package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuestionContentTest {

    private val optionA = QuestionOption(OptionId("a"), "Alternativa A")
    private val optionB = QuestionOption(OptionId("b"), "Alternativa B")

    private fun content(
        statement: String = "O que e polimorfismo?",
        options: List<QuestionOption> = listOf(optionA, optionB),
        correctOption: OptionId = OptionId("a"),
        explanation: String = "Polimorfismo permite tratar tipos diferentes de forma uniforme.",
        category: Category = Category.OOP,
        difficulty: Difficulty = Difficulty.EASY
    ) = QuestionContent(statement, options, correctOption, explanation, category, difficulty)

    @Test
    fun `cria conteudo valido preservando os campos`() {
        val content = content()

        assertEquals("O que e polimorfismo?", content.statement)
        assertEquals(listOf(optionA, optionB), content.options)
        assertEquals(OptionId("a"), content.correctOption)
        assertEquals(Category.OOP, content.category)
        assertEquals(Difficulty.EASY, content.difficulty)
    }

    @Test
    fun `hasOption reconhece apenas ids existentes`() {
        val content = content()

        assertTrue(content.hasOption(OptionId("a")))
        assertTrue(content.hasOption(OptionId("b")))
        assertFalse(content.hasOption(OptionId("c")))
        assertFalse(content.hasOption(OptionId("")))
    }

    @Test
    fun `isCorrect compara com a alternativa correta`() {
        val content = content()

        assertTrue(content.isCorrect(OptionId("a")))
        assertFalse(content.isCorrect(OptionId("b")))
    }

    @Test
    fun `statement em branco e rejeitado`() {
        val error = assertThrows<DomainException> { content(statement = "   ") }

        assertEquals("question.statement.invalid", error.code)
    }

    @Test
    fun `statement vazio e rejeitado`() {
        val error = assertThrows<DomainException> { content(statement = "") }

        assertEquals("question.statement.invalid", error.code)
    }

    @Test
    fun `statement com exatamente 500 caracteres e aceito`() {
        val statement = "x".repeat(500)

        assertEquals(500, content(statement = statement).statement.length)
    }

    @Test
    fun `statement com 501 caracteres e rejeitado`() {
        val error = assertThrows<DomainException> { content(statement = "x".repeat(501)) }

        assertEquals("question.statement.invalid", error.code)
    }

    @Test
    fun `menos de duas opcoes e rejeitado`() {
        val error = assertThrows<DomainException> {
            content(options = listOf(optionA))
        }

        assertEquals("question.options.invalid", error.code)
    }

    @Test
    fun `lista de opcoes vazia e rejeitada`() {
        val error = assertThrows<DomainException> { content(options = emptyList()) }

        assertEquals("question.options.invalid", error.code)
    }

    @Test
    fun `texto de opcao em branco e rejeitado`() {
        val error = assertThrows<DomainException> {
            content(options = listOf(optionA, QuestionOption(OptionId("b"), "  ")))
        }

        assertEquals("question.options.invalid", error.code)
    }

    @Test
    fun `ids de opcao duplicados sao rejeitados`() {
        val error = assertThrows<DomainException> {
            content(options = listOf(optionA, QuestionOption(OptionId("a"), "Duplicada")))
        }

        assertEquals("question.options.invalid", error.code)
    }

    @Test
    fun `correctOption fora das opcoes e rejeitado`() {
        val error = assertThrows<DomainException> {
            content(correctOption = OptionId("z"))
        }

        assertEquals("question.correctOption.invalid", error.code)
    }

    @Test
    fun `explanation em branco e rejeitada`() {
        val error = assertThrows<DomainException> { content(explanation = " ") }

        assertEquals("question.explanation.invalid", error.code)
    }

    @Test
    fun `explanation com exatamente 1000 caracteres e aceita`() {
        val explanation = "x".repeat(1000)

        assertEquals(1000, content(explanation = explanation).explanation.length)
    }

    @Test
    fun `explanation com 1001 caracteres e rejeitada`() {
        val error = assertThrows<DomainException> { content(explanation = "x".repeat(1001)) }

        assertEquals("question.explanation.invalid", error.code)
    }

    @Test
    fun `aceita mais de duas opcoes`() {
        val options = (0..9).map { QuestionOption(OptionId("opt-$it"), "Alternativa $it") }

        val content = content(options = options, correctOption = OptionId("opt-9"))

        assertEquals(10, content.options.size)
        assertTrue(content.isCorrect(OptionId("opt-9")))
    }

    @Test
    fun `duas opcoes com mesmo texto mas ids distintos sao aceitas`() {
        val content = content(
            options = listOf(
                QuestionOption(OptionId("a"), "Mesmo texto"),
                QuestionOption(OptionId("b"), "Mesmo texto")
            )
        )

        assertEquals(2, content.options.size)
    }
}
