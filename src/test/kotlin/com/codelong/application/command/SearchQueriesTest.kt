package com.codelong.application.command

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.domain.valueobject.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class SearchQueriesTest {

    @Test
    fun `QuestionSearchQuery tem valores padrao`() {
        val query = QuestionSearchQuery()

        assertNull(query.status)
        assertNull(query.category)
        assertNull(query.difficulty)
        assertEquals(0, query.page)
        assertEquals(20, query.size)
    }

    @Test
    fun `QuestionSearchQuery converte para o port`() {
        val query = QuestionSearchQuery(
            status = QuestionStatus.INACTIVE,
            category = Category.TESTING,
            difficulty = Difficulty.EXPERT,
            page = 3,
            size = 50
        )

        val search = query.toSearch()

        assertEquals(QuestionStatus.INACTIVE, search.status)
        assertEquals(Category.TESTING, search.category)
        assertEquals(Difficulty.EXPERT, search.difficulty)
        assertEquals(3, search.page)
        assertEquals(50, search.size)
    }

    @Test
    fun `QuestionSearchQuery converte filtros nulos`() {
        val search = QuestionSearchQuery(page = 1, size = 5).toSearch()

        assertNull(search.status)
        assertNull(search.category)
        assertNull(search.difficulty)
        assertEquals(1, search.page)
        assertEquals(5, search.size)
    }

    @Test
    fun `UserSearchQuery tem valores padrao`() {
        val query = UserSearchQuery()

        assertNull(query.status)
        assertNull(query.role)
        assertEquals(0, query.page)
        assertEquals(20, query.size)
    }

    @Test
    fun `UserSearchQuery converte para o port`() {
        val query = UserSearchQuery(
            status = AccountStatus.INACTIVE,
            role = Role.ADMIN,
            page = 2,
            size = 10
        )

        val search = query.toSearch()

        assertEquals(AccountStatus.INACTIVE, search.status)
        assertEquals(Role.ADMIN, search.role)
        assertEquals(2, search.page)
        assertEquals(10, search.size)
    }

    @Test
    fun `UserSearchQuery converte filtros nulos`() {
        val search = UserSearchQuery().toSearch()

        assertNull(search.status)
        assertNull(search.role)
    }

    @Test
    fun `GameSearchQuery tem valores padrao`() {
        val query = GameSearchQuery()

        assertNull(query.status)
        assertEquals(0, query.page)
        assertEquals(20, query.size)
    }

    @Test
    fun `GameSearchQuery aceita status e paginacao`() {
        val query = GameSearchQuery(status = GameStatus.ABANDONED, page = 4, size = 100)

        assertEquals(GameStatus.ABANDONED, query.status)
        assertEquals(4, query.page)
        assertEquals(100, query.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 10, 50, 99, 100])
    fun `GameSearchQuery aceita tamanhos de pagina validos`(size: Int) {
        assertEquals(size, GameSearchQuery(size = size).size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 5, 100, 9999])
    fun `GameSearchQuery aceita paginas nao negativas`(page: Int) {
        assertEquals(page, GameSearchQuery(page = page).page)
    }

    @Test
    fun `GameSearchQuery e um carregador simples sem validacao propria`() {
        val query = GameSearchQuery(status = GameStatus.COMPLETED, page = -1, size = 0)

        assertEquals(GameStatus.COMPLETED, query.status)
        assertEquals(-1, query.page)
        assertEquals(0, query.size)
    }

    @ParameterizedTest
    @CsvSource("0,1", "0,100", "1,20", "10,50")
    fun `QuestionSearchQuery preserva pagina e tamanho`(page: Int, size: Int) {
        val search = QuestionSearchQuery(page = page, size = size).toSearch()

        assertEquals(page, search.page)
        assertEquals(size, search.size)
    }

    @ParameterizedTest
    @CsvSource("0,1", "0,100", "2,30", "7,20")
    fun `UserSearchQuery preserva pagina e tamanho`(page: Int, size: Int) {
        val search = UserSearchQuery(page = page, size = size).toSearch()

        assertEquals(page, search.page)
        assertEquals(size, search.size)
    }
}
