package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.port.QuestionSearch
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.infrastructure.persistence.document.QuestionDocument
import com.codelong.infrastructure.persistence.mapper.QuestionPersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringQuestionDataRepository
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.Optional

class MongoQuestionRepositoryAdapterTest {

    private lateinit var repository: SpringQuestionDataRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: MongoQuestionRepositoryAdapter

    private val question = Fixtures.question(id = "q-1", difficulty = Difficulty.EASY, category = Category.OOP)
    private val document = QuestionPersistenceMapper.toDocument(question)

    @BeforeEach
    fun setUp() {
        repository = mock(SpringQuestionDataRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        adapter = MongoQuestionRepositoryAdapter(repository, mongoTemplate)
    }

    @Test
    fun `save persiste e devolve o dominio`() {
        `when`(repository.save(any(QuestionDocument::class.java))).thenReturn(document)

        val saved = adapter.save(question)

        assertEquals(question.id, saved.id)
        assertEquals(question.statement(), saved.statement())
        assertEquals(question.difficulty(), saved.difficulty())
    }

    @Test
    fun `findById devolve a pergunta quando existe`() {
        `when`(repository.findById("q-1")).thenReturn(Optional.of(document))

        assertEquals("q-1", adapter.findById(QuestionId("q-1"))?.id?.value)
    }

    @Test
    fun `findById devolve nulo quando nao existe`() {
        `when`(repository.findById("q-1")).thenReturn(Optional.empty())

        assertNull(adapter.findById(QuestionId("q-1")))
    }

    @Test
    fun `findAllActive devolve apenas as ativas`() {
        `when`(repository.findByStatus("ACTIVE")).thenReturn(listOf(document))

        val active = adapter.findAllActive()

        assertEquals(1, active.size)
        assertEquals("q-1", active.first().id.value)
        verify(repository).findByStatus("ACTIVE")
    }

    @Test
    fun `findAllActive pode devolver lista vazia`() {
        `when`(repository.findByStatus("ACTIVE")).thenReturn(emptyList())

        assertEquals(0, adapter.findAllActive().size)
    }

    @Test
    fun `countActive delega ao repositorio`() {
        `when`(repository.countByStatus("ACTIVE")).thenReturn(7L)

        assertEquals(7L, adapter.countActive())
    }

    @Test
    fun `search sem filtros devolve pagina`() {
        `when`(mongoTemplate.find(any(Query::class.java), eqClass()))
            .thenReturn(listOf(document))
        `when`(mongoTemplate.count(any(Query::class.java), eqClass())).thenReturn(1L)

        val page = adapter.search(QuestionSearch(page = 0, size = 20))

        assertEquals(1, page.items.size)
        assertEquals(1L, page.totalElements)
        assertEquals("q-1", page.items.first().id.value)
    }

    @Test
    fun `search com todos os filtros aplica criterios`() {
        `when`(mongoTemplate.find(any(Query::class.java), eqClass()))
            .thenReturn(listOf(document))
        `when`(mongoTemplate.count(any(Query::class.java), eqClass())).thenReturn(3L)

        val page = adapter.search(
            QuestionSearch(
                status = QuestionStatus.INACTIVE,
                category = Category.KOTLIN,
                difficulty = Difficulty.MASTER,
                page = 2,
                size = 5
            )
        )

        assertEquals(3L, page.totalElements)
        assertEquals(2, page.page)
        assertEquals(5, page.size)
    }

    @Test
    fun `search com status apenas aplica criterio`() {
        `when`(mongoTemplate.find(any(Query::class.java), eqClass())).thenReturn(emptyList())
        `when`(mongoTemplate.count(any(Query::class.java), eqClass())).thenReturn(0L)

        val page = adapter.search(QuestionSearch(status = QuestionStatus.ACTIVE))

        assertEquals(0, page.items.size)
    }

    @Test
    fun `search com categoria e dificuldade aplica criterios`() {
        `when`(mongoTemplate.find(any(Query::class.java), eqClass())).thenReturn(emptyList())
        `when`(mongoTemplate.count(any(Query::class.java), eqClass())).thenReturn(0L)

        val page = adapter.search(QuestionSearch(category = Category.REST, difficulty = Difficulty.HARD))

        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `deleteById delega ao repositorio`() {
        adapter.deleteById(QuestionId("q-1"))

        verify(repository).deleteById("q-1")
    }

    private fun eqClass() = org.mockito.ArgumentMatchers.eq(QuestionDocument::class.java)
}
