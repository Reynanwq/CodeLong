package com.codelong.domain.valueobject

import com.codelong.domain.valueobject.GameMode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameQuestionTest {

    private val options = listOf(
        QuestionOption(OptionId("a"), "Alternativa correta"),
        QuestionOption(OptionId("b"), "Alternativa incorreta")
    )

    private fun gameQuestion(
        id: String = "q-1",
        correctOption: OptionId = OptionId("a"),
        category: Category = Category.KOTLIN,
        difficulty: Difficulty = Difficulty.MEDIUM
    ) = GameQuestion(
        id = QuestionId(id),
        statement = "Qual palavra-chave define constante em Kotlin?",
        options = options,
        correctOption = correctOption,
        explanation = "val declara uma referencia imutavel.",
        category = category,
        difficulty = difficulty
    )

    @Test
    fun `isCorrect compara com a alternativa correta`() {
        val question = gameQuestion()

        assertTrue(question.isCorrect(OptionId("a")))
        assertFalse(question.isCorrect(OptionId("b")))
        assertFalse(question.isCorrect(OptionId("inexistente")))
    }

    @Test
    fun `hasOption reconhece apenas alternativas existentes`() {
        val question = gameQuestion()

        assertTrue(question.hasOption(OptionId("a")))
        assertTrue(question.hasOption(OptionId("b")))
        assertFalse(question.hasOption(OptionId("c")))
    }

    @Test
    fun `publicView nao expoe a resposta correta nem a explicacao`() {
        val publicView = gameQuestion().publicView()

        assertEquals(QuestionId("q-1"), publicView.id)
        assertEquals("Qual palavra-chave define constante em Kotlin?", publicView.statement)
        assertEquals(options, publicView.options)
        assertEquals(Category.KOTLIN, publicView.category)
        assertEquals(Difficulty.MEDIUM, publicView.difficulty)
    }

    @Test
    fun `publicView preserva os ids das alternativas`() {
        val publicView = gameQuestion().publicView()

        assertEquals(listOf(OptionId("a"), OptionId("b")), publicView.options.map { it.id })
    }

    @Test
    fun `publicView e um tipo QuestionPublic`() {
        val publicView: QuestionPublic = gameQuestion().publicView()

        assertTrue(publicView is QuestionPublic)
    }

    @Test
    fun `GameQuestion tem igualdade por valor`() {
        assertEquals(gameQuestion(), gameQuestion())
        assertEquals(gameQuestion().hashCode(), gameQuestion().hashCode())
    }

    @Test
    fun `GameQuestion difere quando o conteudo difere`() {
        val base = gameQuestion()
        val differentId = gameQuestion(id = "q-2")
        val differentDifficulty = gameQuestion(difficulty = Difficulty.HARD)

        assertFalse(base == differentId)
        assertFalse(base == differentDifficulty)
    }

    @Test
    fun `copy permite alterar apenas um campo`() {
        val copy = gameQuestion().copy(category = Category.JAVA)

        assertEquals(Category.JAVA, copy.category)
        assertEquals(Difficulty.MEDIUM, copy.difficulty)
        assertEquals(QuestionId("q-1"), copy.id)
    }

    @Test
    fun `QuestionPublic tem igualdade por valor`() {
        val first = gameQuestion().publicView()
        val second = gameQuestion().publicView()

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `GameSetup agrupa usuario username e perguntas`() {
        val setup = GameSetup(
            userId = UserId("u-1"),
            username = "alice",
            questions = listOf(gameQuestion()),
            mode = GameMode.CLASSIC
        )

        assertEquals(UserId("u-1"), setup.userId)
        assertEquals("alice", setup.username)
        assertEquals(1, setup.questions.size)
        assertEquals(setup, setup.copy())
    }
}
