package com.codelong.infrastructure.persistence.document

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

class PersistenceDocumentsTest {

    @Test
    fun `GameDocument mapeia a colecao games`() {
        val annotation = GameDocument::class.java.getAnnotation(Document::class.java)

        assertEquals("games", annotation.collection)
    }

    @Test
    fun `GameDocument declara os indices de ranking e de usuario`() {
        val indexes = GameDocument::class.java.getAnnotationsByType(CompoundIndex::class.java)
        val names = indexes.map { it.name }.toSet()

        assertEquals(2, indexes.size)
        assertTrue(names.contains("game_ranking_idx"))
        assertTrue(names.contains("game_user_idx"))
    }

    @Test
    fun `GameDocument usa os valores padrao`() {
        val document = GameDocument()

        assertEquals("", document.id)
        assertEquals("", document.userId)
        assertEquals("", document.username)
        assertEquals("", document.status)
        assertEquals(Instant.EPOCH, document.startedAt)
        assertNull(document.completedAt)
        assertEquals(0, document.currentQuestionIndex)
        assertEquals(Instant.EPOCH, document.currentQuestionDeadline)
        assertTrue(document.questions.isEmpty())
        assertTrue(document.answers.isEmpty())
        assertEquals(0, document.score)
        assertEquals(0, document.correctAnswers)
        assertEquals(0, document.wrongAnswers)
        assertEquals(0L, document.version)
    }

    @Test
    fun `GameDocument usa Id e Version do Spring Data`() {
        assertTrue(GameDocument::class.java.getDeclaredField("id").isAnnotationPresent(Id::class.java))
        assertTrue(GameDocument::class.java.getDeclaredField("version").isAnnotationPresent(Version::class.java))
    }

    @Test
    fun `GameQuestionDocument usa os valores padrao`() {
        val document = GameQuestionDocument()

        assertEquals("", document.id)
        assertEquals("", document.statement)
        assertTrue(document.options.isEmpty())
        assertEquals("", document.correctOption)
        assertEquals("", document.explanation)
        assertEquals("", document.category)
        assertEquals("", document.difficulty)
    }

    @Test
    fun `AnswerDocument usa os valores padrao`() {
        val document = AnswerDocument()

        assertEquals(0, document.questionIndex)
        assertEquals("", document.questionId)
        assertNull(document.chosenOption)
        assertEquals(false, document.timedOut)
        assertEquals(false, document.correct)
        assertEquals(0, document.earnedPoints)
        assertEquals(Instant.EPOCH, document.answeredAt)
    }

    @Test
    fun `RankEntryDocument usa os valores padrao`() {
        val document = RankEntryDocument()

        assertEquals("", document.userId)
        assertEquals("", document.username)
        assertEquals(0, document.score)
        assertEquals(0, document.correctAnswers)
        assertEquals(0L, document.totalTimeMillis)
        assertEquals(Instant.EPOCH, document.achievedAt)
    }

    @Test
    fun `UserDocument mapeia a colecao users`() {
        val annotation = UserDocument::class.java.getAnnotation(Document::class.java)

        assertEquals("users", annotation.collection)
    }

    @Test
    fun `UserDocument declara indices unicos de username e email`() {
        val username = UserDocument::class.java.getDeclaredField("username").getAnnotation(Indexed::class.java)
        val email = UserDocument::class.java.getDeclaredField("email").getAnnotation(Indexed::class.java)

        assertTrue(username.unique)
        assertTrue(email.unique)
    }

    @Test
    fun `UserDocument usa os valores padrao`() {
        val document = UserDocument()

        assertEquals("", document.id)
        assertEquals("", document.username)
        assertEquals("", document.email)
        assertEquals("", document.passwordHash)
        assertEquals("", document.role)
        assertEquals("", document.status)
        assertEquals(Instant.EPOCH, document.createdAt)
        assertEquals(Instant.EPOCH, document.updatedAt)
    }

    @Test
    fun `QuestionDocument mapeia a colecao questions`() {
        val annotation = QuestionDocument::class.java.getAnnotation(Document::class.java)

        assertEquals("questions", annotation.collection)
    }

    @Test
    fun `QuestionDocument declara o indice composto de busca`() {
        val indexes = QuestionDocument::class.java.getAnnotationsByType(CompoundIndex::class.java)

        assertEquals(1, indexes.size)
        assertEquals("question_search_idx", indexes.first().name)
    }

    @Test
    fun `QuestionDocument indexa createdAt`() {
        val createdAt = QuestionDocument::class.java.getDeclaredField("createdAt").getAnnotation(Indexed::class.java)

        assertEquals(false, createdAt.unique)
    }

    @Test
    fun `QuestionDocument usa os valores padrao`() {
        val document = QuestionDocument()

        assertEquals("", document.id)
        assertEquals("", document.statement)
        assertTrue(document.options.isEmpty())
        assertEquals("", document.correctOption)
        assertEquals("", document.explanation)
        assertEquals("", document.category)
        assertEquals("", document.difficulty)
        assertEquals("", document.status)
        assertEquals(Instant.EPOCH, document.createdAt)
        assertEquals(Instant.EPOCH, document.updatedAt)
    }

    @Test
    fun `OptionDocument usa os valores padrao`() {
        val document = OptionDocument()

        assertEquals("", document.id)
        assertEquals("", document.text)
    }

    @Test
    fun `documentos tem igualdade por valor`() {
        assertEquals(OptionDocument("a", "A"), OptionDocument("a", "A"))
        assertEquals(GameQuestionDocument(id = "q-1"), GameQuestionDocument(id = "q-1"))
        assertEquals(AnswerDocument(questionIndex = 1), AnswerDocument(questionIndex = 1))
        assertEquals(RankEntryDocument(userId = "u-1"), RankEntryDocument(userId = "u-1"))
        assertEquals(UserDocument(id = "u-1"), UserDocument(id = "u-1"))
        assertEquals(QuestionDocument(id = "q-1"), QuestionDocument(id = "q-1"))
    }
}
