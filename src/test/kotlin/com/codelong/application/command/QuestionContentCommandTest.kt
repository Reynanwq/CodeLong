package com.codelong.application.command

import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class QuestionContentCommandTest {

    private fun options() = listOf(
        QuestionOptionCommand("a", "Alternativa A"),
        QuestionOptionCommand("b", "Alternativa B")
    )

    private fun createCommand(
        statement: String = "O que e polimorfismo?",
        options: List<QuestionOptionCommand> = options(),
        correctOption: String = "a",
        explanation: String = "Explicacao.",
        category: Category = Category.OOP,
        difficulty: Difficulty = Difficulty.EASY
    ) = CreateQuestionCommand(statement, options, correctOption, explanation, category, difficulty)

    @Test
    fun `QuestionOptionCommand converte para o dominio`() {
        val option = QuestionOptionCommand("a", "Alternativa A").toDomain()

        assertEquals(OptionId("a"), option.id)
        assertEquals("Alternativa A", option.text)
    }

    @Test
    fun `QuestionOptionCommand remove espacos do id e do texto`() {
        val option = QuestionOptionCommand("  a  ", "  Alternativa A  ").toDomain()

        assertEquals(OptionId("a"), option.id)
        assertEquals("Alternativa A", option.text)
    }

    @Test
    fun `toContent remove espacos do enunciado e da explicacao`() {
        val content = createCommand(
            statement = "  Enunciado  ",
            explanation = "  Explicacao  "
        ).toContent()

        assertEquals("Enunciado", content.statement)
        assertEquals("Explicacao", content.explanation)
    }

    @Test
    fun `toContent remove espacos do correctOption`() {
        val content = createCommand(correctOption = "  a  ").toContent()

        assertEquals(OptionId("a"), content.correctOption)
        assertTrue(content.hasOption(OptionId("a")))
    }

    @Test
    fun `toContent converte todas as opcoes`() {
        val content = createCommand().toContent()

        assertEquals(2, content.options.size)
        assertEquals(listOf(OptionId("a"), OptionId("b")), content.options.map { it.id })
        assertEquals(listOf("Alternativa A", "Alternativa B"), content.options.map { it.text })
    }

    @Test
    fun `toContent preserva categoria e dificuldade`() {
        val content = createCommand(category = Category.KAFKA, difficulty = Difficulty.MASTER).toContent()

        assertEquals(Category.KAFKA, content.category)
        assertEquals(Difficulty.MASTER, content.difficulty)
    }

    @Test
    fun `UpdateQuestionCommand tambem converte para conteudo`() {
        val command = UpdateQuestionCommand(
            questionId = QuestionId("q-1"),
            statement = "  Atualizado  ",
            options = options(),
            correctOption = "b",
            explanation = "  Nova explicacao  ",
            category = Category.SPRING,
            difficulty = Difficulty.HARD
        )

        val content = command.toContent()

        assertEquals(QuestionId("q-1"), command.questionId)
        assertEquals("Atualizado", content.statement)
        assertEquals(OptionId("b"), content.correctOption)
        assertEquals(Category.SPRING, content.category)
        assertEquals(Difficulty.HARD, content.difficulty)
    }

    @Test
    fun `toContent rejeita enunciado em branco`() {
        val error = assertThrows<InvalidInputException> {
            createCommand(statement = "   ").toContent()
        }

        assertEquals("question.statement.invalid", error.code)
    }

    @Test
    fun `toContent rejeita menos de duas opcoes`() {
        val error = assertThrows<InvalidInputException> {
            createCommand(options = listOf(QuestionOptionCommand("a", "Unica"))).toContent()
        }

        assertEquals("question.options.invalid", error.code)
    }

    @Test
    fun `toContent rejeita alternativa correta inexistente`() {
        val error = assertThrows<InvalidInputException> {
            createCommand(correctOption = "z").toContent()
        }

        assertEquals("question.correctOption.invalid", error.code)
    }

    @Test
    fun `toContent rejeita explicacao em branco`() {
        val error = assertThrows<InvalidInputException> {
            createCommand(explanation = " ").toContent()
        }

        assertEquals("question.explanation.invalid", error.code)
    }

    @Test
    fun `toContent rejeita ids de opcao duplicados`() {
        val error = assertThrows<InvalidInputException> {
            createCommand(
                options = listOf(
                    QuestionOptionCommand("a", "Primeira"),
                    QuestionOptionCommand(" a ", "Segunda")
                )
            ).toContent()
        }

        assertEquals("question.options.invalid", error.code)
    }

    @ParameterizedTest
    @ValueSource(strings = ["a", "b", "opt-1", "A", "1"])
    fun `toContent aceita ids de opcao variados quando existirem`(optionId: String) {
        val content = createCommand(
            options = listOf(
                QuestionOptionCommand(optionId, "Alternativa"),
                QuestionOptionCommand("outro", "Outra")
            ),
            correctOption = optionId
        ).toContent()

        assertEquals(OptionId(optionId), content.correctOption)
    }
}
