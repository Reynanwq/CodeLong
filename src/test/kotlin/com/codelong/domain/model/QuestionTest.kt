package com.codelong.domain.model

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuestionTest {

    private val now: Instant = Fixtures.NOW
    private val later: Instant = now.plusSeconds(3600)

    @Test
    fun `create inicia a pergunta ativa com datas iguais`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.MEDIUM, category = Category.KOTLIN)

        assertEquals(QuestionId("q-1"), question.id)
        assertEquals(QuestionStatus.ACTIVE, question.status())
        assertEquals(now, question.createdAt)
        assertEquals(now, question.updatedAt())
        assertTrue(question.isActive())
    }

    @Test
    fun `expoe todo o conteudo da pergunta`() {
        val question = Fixtures.question(difficulty = Difficulty.EXPERT, category = Category.SOLID)

        assertEquals("O que e polimorfismo?", question.statement())
        assertEquals(3, question.options().size)
        assertEquals(OptionId("opt-0"), question.correctOption())
        assertEquals(
            "Polimorfismo permite tratar objetos de tipos diferentes de forma uniforme.",
            question.explanation()
        )
        assertEquals(Category.SOLID, question.category())
        assertEquals(Difficulty.EXPERT, question.difficulty())
    }

    @Test
    fun `hasOption reconhece alternativas existentes`() {
        val question = Fixtures.question()

        assertTrue(question.hasOption(OptionId("opt-0")))
        assertTrue(question.hasOption(OptionId("opt-1")))
        assertTrue(question.hasOption(OptionId("opt-2")))
        assertFalse(question.hasOption(OptionId("opt-3")))
        assertFalse(question.hasOption(OptionId("")))
    }

    @Test
    fun `isCorrectOption identifica a alternativa correta`() {
        val question = Fixtures.question()

        assertTrue(question.isCorrectOption(OptionId("opt-0")))
        assertFalse(question.isCorrectOption(OptionId("opt-1")))
        assertFalse(question.isCorrectOption(OptionId("opt-2")))
    }

    @Test
    fun `deactivate muda o status e atualiza a data`() {
        val question = Fixtures.question()

        val returned = question.deactivate(later)

        assertSame(question, returned)
        assertEquals(QuestionStatus.INACTIVE, question.status())
        assertFalse(question.isActive())
        assertEquals(later, question.updatedAt())
        assertEquals(now, question.createdAt)
    }

    @Test
    fun `activate volta o status para ACTIVE`() {
        val question = Fixtures.question().deactivate(now)

        val returned = question.activate(later)

        assertSame(question, returned)
        assertEquals(QuestionStatus.ACTIVE, question.status())
        assertTrue(question.isActive())
        assertEquals(later, question.updatedAt())
    }

    @Test
    fun `update substitui o conteudo e atualiza a data`() {
        val question = Fixtures.question()
        val newContent = Fixtures.questionContent(
            statement = "Novo enunciado?",
            optionCount = 4,
            correctIndex = 3,
            difficulty = Difficulty.MASTER,
            category = Category.TESTING
        )

        val returned = question.update(newContent, later)

        assertSame(question, returned)
        assertEquals("Novo enunciado?", question.statement())
        assertEquals(4, question.options().size)
        assertEquals(OptionId("opt-3"), question.correctOption())
        assertEquals(Category.TESTING, question.category())
        assertEquals(Difficulty.MASTER, question.difficulty())
        assertEquals(later, question.updatedAt())
    }

    @Test
    fun `snapshot copia o conteudo para a partida`() {
        val question = Fixtures.question(id = "q-9", difficulty = Difficulty.HARD, category = Category.DATABASE)

        val snapshot = question.snapshot()

        assertEquals(QuestionId("q-9"), snapshot.id)
        assertEquals(question.statement(), snapshot.statement)
        assertEquals(question.options(), snapshot.options)
        assertEquals(question.correctOption(), snapshot.correctOption)
        assertEquals(question.explanation(), snapshot.explanation)
        assertEquals(question.category(), snapshot.category)
        assertEquals(question.difficulty(), snapshot.difficulty)
    }

    @Test
    fun `snapshot nao muda quando a pergunta e editada depois`() {
        val question = Fixtures.question(difficulty = Difficulty.EASY)
        val snapshot = question.snapshot()

        question.update(
            Fixtures.questionContent(statement = "Alterado?", difficulty = Difficulty.MASTER),
            later
        )

        assertEquals("O que e polimorfismo?", snapshot.statement)
        assertEquals(Difficulty.EASY, snapshot.difficulty)
    }

    @Test
    fun `state reflete o estado atual da pergunta`() {
        val question = Fixtures.question(id = "q-1").deactivate(later)

        val state = question.state()

        assertEquals(QuestionId("q-1"), state.id)
        assertEquals(QuestionStatus.INACTIVE, state.status)
        assertEquals(now, state.createdAt)
        assertEquals(later, state.updatedAt)
        assertEquals(question.statement(), state.content.statement)
    }

    @Test
    fun `reconstitute preserva conteudo status e datas`() {
        val original = Fixtures.question(id = "q-1", difficulty = Difficulty.MEDIUM)
            .deactivate(later)

        val restored = Question.reconstitute(original.state())

        assertEquals(original.id, restored.id)
        assertEquals(original.statement(), restored.statement())
        assertEquals(original.options(), restored.options())
        assertEquals(original.correctOption(), restored.correctOption())
        assertEquals(original.explanation(), restored.explanation())
        assertEquals(original.category(), restored.category())
        assertEquals(original.difficulty(), restored.difficulty())
        assertEquals(QuestionStatus.INACTIVE, restored.status())
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt(), restored.updatedAt())
    }
}
