package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.infrastructure.persistence.document.OptionDocument
import com.codelong.infrastructure.persistence.document.QuestionDocument
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuestionPersistenceMapperTest {

    @Test
    fun `toDocument copia todos os campos da pergunta`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.EXPERT, category = Category.KAFKA)

        val document = QuestionPersistenceMapper.toDocument(question)

        assertEquals("q-1", document.id)
        assertEquals(question.statement, document.statement)
        assertEquals(3, document.options.size)
        assertEquals("opt-0", document.correctOption)
        assertEquals(question.explanation, document.explanation)
        assertEquals("KAFKA", document.category)
        assertEquals("EXPERT", document.difficulty)
        assertEquals("ACTIVE", document.status)
        assertEquals(Fixtures.NOW, document.createdAt)
        assertEquals(Fixtures.NOW, document.updatedAt)
    }

    @Test
    fun `toDocument converte as alternativas`() {
        val document = QuestionPersistenceMapper.toDocument(Fixtures.question(id = "q-1"))

        assertEquals(listOf("opt-0", "opt-1", "opt-2"), document.options.map { it.id })
        assertEquals("Alternativa 0", document.options.first().text)
    }

    @Test
    fun `toDomain reconstroi a pergunta`() {
        val document = QuestionDocument(
            id = "q-1",
            statement = "Enunciado?",
            options = listOf(OptionDocument("a", "A"), OptionDocument("b", "B")),
            correctOption = "a",
            explanation = "Explicacao.",
            category = "SOLID",
            difficulty = "HARD",
            status = "INACTIVE",
            createdAt = Fixtures.NOW,
            updatedAt = Fixtures.NOW.plusSeconds(60)
        )

        val question = QuestionPersistenceMapper.toDomain(document)

        assertEquals("q-1", question.id.value)
        assertEquals("Enunciado?", question.statement)
        assertEquals(2, question.options.size)
        assertEquals(OptionId("a"), question.correctOption)
        assertEquals("Explicacao.", question.explanation)
        assertEquals(Category.SOLID, question.category)
        assertEquals(Difficulty.HARD, question.difficulty)
        assertEquals(QuestionStatus.INACTIVE, question.status)
        assertFalse(question.isActive)
    }

    @Test
    fun `round trip preserva o estado`() {
        val original = Fixtures.question(id = "q-1", difficulty = Difficulty.MASTER, category = Category.GIT)

        val restored = QuestionPersistenceMapper.toDomain(QuestionPersistenceMapper.toDocument(original))

        assertEquals(original.id, restored.id)
        assertEquals(original.statement, restored.statement)
        assertEquals(original.options, restored.options)
        assertEquals(original.correctOption, restored.correctOption)
        assertEquals(original.explanation, restored.explanation)
        assertEquals(original.category, restored.category)
        assertEquals(original.difficulty, restored.difficulty)
        assertEquals(original.status, restored.status)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
    }

    @Test
    fun `round trip preserva pergunta inativa`() {
        val original = Fixtures.question(id = "q-1").deactivate(Fixtures.NOW.plusSeconds(30))

        val restored = QuestionPersistenceMapper.toDomain(QuestionPersistenceMapper.toDocument(original))

        assertEquals(QuestionStatus.INACTIVE, restored.status)
        assertFalse(restored.isActive)
        assertEquals(Fixtures.NOW.plusSeconds(30), restored.updatedAt)
    }

    @Test
    fun `toDomain aceita categoria em caixa baixa`() {
        val document = QuestionDocument(
            id = "q-1",
            statement = "Enunciado?",
            options = listOf(OptionDocument("a", "A"), OptionDocument("b", "B")),
            correctOption = "a",
            explanation = "Explicacao.",
            category = "kotlin",
            difficulty = "EASY",
            status = "ACTIVE",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )

        assertEquals(Category.KOTLIN, QuestionPersistenceMapper.toDomain(document).category)
    }

    @Test
    fun `toDocument de pergunta ativa marca ACTIVE`() {
        val document = QuestionPersistenceMapper.toDocument(Fixtures.question(id = "q-1"))

        assertEquals("ACTIVE", document.status)
        assertTrue(QuestionPersistenceMapper.toDomain(document).isActive)
    }
}
