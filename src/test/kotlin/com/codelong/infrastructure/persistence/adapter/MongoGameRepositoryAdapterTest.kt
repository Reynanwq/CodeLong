package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.exception.DomainException

import com.codelong.domain.port.GameSearch
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.mapper.GamePersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringGameDataRepository
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.Optional

class MongoGameRepositoryAdapterTest {

    private lateinit var repository: SpringGameDataRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: MongoGameRepositoryAdapter

    private val game = Fixtures.game(id = "g-1", userId = "u-1", username = "alice")
    private val document = GamePersistenceMapper.toDocument(game)

    @BeforeEach
    fun setUp() {
        repository = mock(SpringGameDataRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        adapter = MongoGameRepositoryAdapter(repository, mongoTemplate)
    }

    @Test
    fun `save persiste e devolve o dominio`() {
        `when`(repository.save(any(GameDocument::class.java))).thenReturn(document)

        val saved = adapter.save(game)

        assertEquals(game.id, saved.id)
        assertEquals(game.userId, saved.userId)
        assertEquals(game.username, saved.username)
    }

    @Test
    fun `save traduz falha de lock otimista`() {
        `when`(repository.save(any(GameDocument::class.java)))
            .thenThrow(OptimisticLockingFailureException("conflito"))

        assertThrows<DomainException> { adapter.save(game) }
    }

    @Test
    fun `findById devolve a partida quando existe`() {
        `when`(repository.findById("g-1")).thenReturn(Optional.of(document))

        assertEquals("g-1", adapter.findById(GameId("g-1"))?.id?.value)
    }

    @Test
    fun `findById devolve nulo quando nao existe`() {
        `when`(repository.findById("g-1")).thenReturn(Optional.empty())

        assertNull(adapter.findById(GameId("g-1")))
    }

    @Test
    fun `findInProgressByUserId devolve a partida em andamento`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(listOf(document))

        assertEquals("g-1", adapter.findInProgressByUserId(UserId("u-1"))?.id?.value)
    }

    @Test
    fun `findInProgressByUserId devolve nulo quando nao ha partida`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(emptyList())

        assertNull(adapter.findInProgressByUserId(UserId("u-1")))
    }

    @Test
    fun `findInProgressByUserId usa apenas o primeiro resultado`() {
        val second = GamePersistenceMapper.toDocument(Fixtures.game(id = "g-2", userId = "u-1"))
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(listOf(document, second))

        assertEquals("g-1", adapter.findInProgressByUserId(UserId("u-1"))?.id?.value)
    }

    @Test
    fun `search sem filtro devolve pagina`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(listOf(document))
        `when`(mongoTemplate.count(any(Query::class.java), eq(GameDocument::class.java))).thenReturn(1L)

        val page = adapter.search(GameSearch(userId = UserId("u-1"), page = 0, size = 20))

        assertEquals(1, page.items.size)
        assertEquals(1L, page.totalElements)
        assertEquals("g-1", page.items.first().id.value)
    }

    @Test
    fun `search com status aplica criterio`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(emptyList())
        `when`(mongoTemplate.count(any(Query::class.java), eq(GameDocument::class.java))).thenReturn(0L)

        val page = adapter.search(
            GameSearch(userId = UserId("u-1"), status = GameStatus.COMPLETED, page = 1, size = 5)
        )

        assertEquals(0, page.items.size)
        assertEquals(1, page.page)
        assertEquals(5, page.size)
    }

    @Test
    fun `search devolve partidas mapeadas para o dominio`() {
        val completed = Fixtures.game(id = "g-2", userId = "u-1", difficulties = listOf(Difficulty.EASY))
        completed.answer(completed.currentQuestion().correctOption, Fixtures.NOW)
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(listOf(GamePersistenceMapper.toDocument(completed)))
        `when`(mongoTemplate.count(any(Query::class.java), eq(GameDocument::class.java))).thenReturn(1L)

        val page = adapter.search(GameSearch(userId = UserId("u-1")))

        assertEquals(GameStatus.COMPLETED, page.items.first().status())
        assertEquals(Difficulty.EASY.points, page.items.first().score())
    }

    @Test
    fun `search com lista vazia devolve pagina vazia`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(GameDocument::class.java)))
            .thenReturn(emptyList())
        `when`(mongoTemplate.count(any(Query::class.java), eq(GameDocument::class.java))).thenReturn(0L)

        val page = adapter.search(GameSearch(userId = UserId("u-1")))

        assertEquals(0, page.items.size)
        assertEquals(0L, page.totalElements)
    }
}
