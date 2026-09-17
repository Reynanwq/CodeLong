package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.port.UserSearch
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.persistence.document.UserDocument
import com.codelong.infrastructure.persistence.mapper.UserPersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringUserDataRepository
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.Optional

class MongoUserRepositoryAdapterTest {

    private lateinit var repository: SpringUserDataRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: MongoUserRepositoryAdapter

    private val user = Fixtures.user(id = "u-1", username = "alice")
    private val document = UserPersistenceMapper.toDocument(user)

    @BeforeEach
    fun setUp() {
        repository = mock(SpringUserDataRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        adapter = MongoUserRepositoryAdapter(repository, mongoTemplate)
    }

    @Test
    fun `save persiste e devolve o dominio`() {
        `when`(repository.save(any(UserDocument::class.java))).thenReturn(document)

        val saved = adapter.save(user)

        assertEquals(user.id, saved.id)
        assertEquals(user.username, saved.username)
        assertEquals(user.email, saved.email)
        verify(repository).save(any(UserDocument::class.java))
    }

    @Test
    fun `findById devolve o usuario quando existe`() {
        `when`(repository.findById("u-1")).thenReturn(Optional.of(document))

        val found = adapter.findById(UserId("u-1"))

        assertEquals("u-1", found?.id?.value)
        assertEquals("alice", found?.username?.value)
    }

    @Test
    fun `findById devolve nulo quando nao existe`() {
        `when`(repository.findById("u-1")).thenReturn(Optional.empty())

        assertNull(adapter.findById(UserId("u-1")))
    }

    @Test
    fun `findByUsername devolve o usuario`() {
        `when`(repository.findByUsername("alice")).thenReturn(document)

        assertEquals("u-1", adapter.findByUsername(Username.of("alice"))?.id?.value)
    }

    @Test
    fun `findByUsername devolve nulo quando nao existe`() {
        `when`(repository.findByUsername(anyString())).thenReturn(null)

        assertNull(adapter.findByUsername(Username.of("ninguem")))
    }

    @Test
    fun `findByEmail devolve o usuario`() {
        `when`(repository.findByEmail("alice@codelong.dev")).thenReturn(document)

        assertEquals("u-1", adapter.findByEmail(Email.of("alice@codelong.dev"))?.id?.value)
    }

    @Test
    fun `findByEmail devolve nulo quando nao existe`() {
        `when`(repository.findByEmail(anyString())).thenReturn(null)

        assertNull(adapter.findByEmail(Email.of("ninguem@codelong.dev")))
    }

    @Test
    fun `existsByUsername delega ao repositorio`() {
        `when`(repository.existsByUsername("alice")).thenReturn(true)

        assertEquals(true, adapter.existsByUsername(Username.of("alice")))
    }

    @Test
    fun `existsByEmail delega ao repositorio`() {
        `when`(repository.existsByEmail("alice@codelong.dev")).thenReturn(false)

        assertEquals(false, adapter.existsByEmail(Email.of("alice@codelong.dev")))
    }

    @Test
    fun `search sem filtros devolve pagina`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(UserDocument::class.java)))
            .thenReturn(listOf(document))
        `when`(mongoTemplate.count(any(Query::class.java), eq(UserDocument::class.java))).thenReturn(1L)

        val page = adapter.search(UserSearch(page = 0, size = 20))

        assertEquals(1, page.items.size)
        assertEquals(1L, page.totalElements)
        assertEquals(0, page.page)
        assertEquals(20, page.size)
        assertEquals("alice", page.items.first().username.value)
    }

    @Test
    fun `search com status e papel aplica filtros`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(UserDocument::class.java)))
            .thenReturn(listOf(document))
        `when`(mongoTemplate.count(any(Query::class.java), eq(UserDocument::class.java))).thenReturn(5L)

        val page = adapter.search(
            UserSearch(status = AccountStatus.INACTIVE, role = Role.ADMIN, page = 1, size = 10)
        )

        assertEquals(5L, page.totalElements)
        assertEquals(1, page.page)
        assertEquals(10, page.size)
    }

    @Test
    fun `search com lista vazia devolve pagina vazia`() {
        `when`(mongoTemplate.find(any(Query::class.java), eq(UserDocument::class.java)))
            .thenReturn(emptyList())
        `when`(mongoTemplate.count(any(Query::class.java), eq(UserDocument::class.java))).thenReturn(0L)

        val page = adapter.search(UserSearch())

        assertEquals(0, page.items.size)
        assertEquals(0L, page.totalElements)
    }
}
