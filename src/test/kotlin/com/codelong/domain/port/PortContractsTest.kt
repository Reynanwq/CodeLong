package com.codelong.domain.port

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PortContractsTest {

    private val userId = UserId("u-1")

    @Test
    fun `GameSearch tem valores padrao`() {
        val search = GameSearch(userId = userId)

        assertEquals(userId, search.userId)
        assertNull(search.status)
        assertEquals(0, search.page)
        assertEquals(20, search.size)
    }

    @Test
    fun `GameSearch aceita status e paginacao`() {
        val search = GameSearch(userId = userId, status = GameStatus.COMPLETED, page = 2, size = 30)

        assertEquals(GameStatus.COMPLETED, search.status)
        assertEquals(2, search.page)
        assertEquals(30, search.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 20, 99, 100])
    fun `GameSearch aceita tamanhos validos`(size: Int) {
        assertEquals(size, GameSearch(userId = userId, size = size).size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 101, 500, Int.MIN_VALUE])
    fun `GameSearch rejeita tamanhos invalidos`(size: Int) {
        val error = assertThrows<IllegalArgumentException> {
            GameSearch(userId = userId, size = size)
        }

        assertEquals("size must be between 1 and 100", error.message)
    }

    @Test
    fun `GameSearch rejeita pagina negativa`() {
        val error = assertThrows<IllegalArgumentException> {
            GameSearch(userId = userId, page = -1)
        }

        assertEquals("page must be >= 0", error.message)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 7, 100, 10_000])
    fun `GameSearch aceita paginas nao negativas`(page: Int) {
        assertEquals(page, GameSearch(userId = userId, page = page).page)
    }

    @Test
    fun `UserSearch tem valores padrao`() {
        val search = UserSearch()

        assertNull(search.status)
        assertNull(search.role)
        assertEquals(0, search.page)
        assertEquals(20, search.size)
    }

    @Test
    fun `UserSearch aceita filtros e paginacao`() {
        val search = UserSearch(status = AccountStatus.INACTIVE, role = Role.ADMIN, page = 1, size = 50)

        assertEquals(AccountStatus.INACTIVE, search.status)
        assertEquals(Role.ADMIN, search.role)
        assertEquals(1, search.page)
        assertEquals(50, search.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 101, Int.MAX_VALUE])
    fun `UserSearch rejeita tamanhos invalidos`(size: Int) {
        assertThrows<IllegalArgumentException> { UserSearch(size = size) }
    }

    @Test
    fun `UserSearch rejeita pagina negativa`() {
        val error = assertThrows<IllegalArgumentException> { UserSearch(page = -5) }

        assertEquals("page must be >= 0", error.message)
    }

    @Test
    fun `QuestionSearch tem valores padrao`() {
        val search = QuestionSearch()

        assertNull(search.status)
        assertNull(search.category)
        assertNull(search.difficulty)
        assertEquals(0, search.page)
        assertEquals(20, search.size)
    }

    @Test
    fun `QuestionSearch aceita filtros e paginacao`() {
        val search = QuestionSearch(
            status = QuestionStatus.ACTIVE,
            category = Category.DOCKER,
            difficulty = Difficulty.VERY_HARD,
            page = 3,
            size = 15
        )

        assertEquals(QuestionStatus.ACTIVE, search.status)
        assertEquals(Category.DOCKER, search.category)
        assertEquals(Difficulty.VERY_HARD, search.difficulty)
        assertEquals(3, search.page)
        assertEquals(15, search.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 101, Int.MIN_VALUE])
    fun `QuestionSearch rejeita tamanhos invalidos`(size: Int) {
        assertThrows<IllegalArgumentException> { QuestionSearch(size = size) }
    }

    @Test
    fun `QuestionSearch rejeita pagina negativa`() {
        val error = assertThrows<IllegalArgumentException> { QuestionSearch(page = -1) }

        assertEquals("page must be >= 0", error.message)
    }

    @Test
    fun `GamePage carrega itens e metadados`() {
        val games = listOf(Fixtures.game(id = "g-1"), Fixtures.game(id = "g-2"))

        val page = GamePage(items = games, totalElements = 5L, page = 1, size = 2)

        assertEquals(2, page.items.size)
        assertEquals(5L, page.totalElements)
        assertEquals(1, page.page)
        assertEquals(2, page.size)
    }

    @Test
    fun `UserPage carrega itens e metadados`() {
        val users = listOf(Fixtures.user(id = "u-1"))

        val page = UserPage(items = users, totalElements = 1L, page = 0, size = 20)

        assertEquals(1, page.items.size)
        assertEquals(1L, page.totalElements)
        assertEquals(0, page.page)
        assertEquals(20, page.size)
    }

    @Test
    fun `QuestionPage carrega itens e metadados`() {
        val questions = listOf(Fixtures.question(id = "q-1"))

        val page = QuestionPage(items = questions, totalElements = 10L, page = 2, size = 5)

        assertEquals(1, page.items.size)
        assertEquals(10L, page.totalElements)
        assertEquals(2, page.page)
        assertEquals(5, page.size)
    }

    @Test
    fun `paginas aceitam lista vazia`() {
        val gamePage = GamePage(items = emptyList(), totalElements = 0L, page = 0, size = 20)
        val userPage = UserPage(items = emptyList(), totalElements = 0L, page = 0, size = 20)
        val questionPage = QuestionPage(items = emptyList(), totalElements = 0L, page = 0, size = 20)

        assertTrue(gamePage.items.isEmpty())
        assertTrue(userPage.items.isEmpty())
        assertTrue(questionPage.items.isEmpty())
    }
}
