package com.codelong.application.usecase

import com.codelong.application.command.GameSearchQuery
import com.codelong.domain.model.Game
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryGameRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameHistoryUseCaseTest {

    private lateinit var repository: InMemoryGameRepository
    private lateinit var listUseCase: ListGamesUseCase
    private lateinit var inProgressUseCase: GetInProgressGameUseCase

    @BeforeEach
    fun setUp() {
        repository = InMemoryGameRepository()
        listUseCase = ListGamesUseCase(repository)
        inProgressUseCase = GetInProgressGameUseCase(repository)

        repository.save(Fixtures.game(id = "g-1", userId = "u-1", username = "alice"))
        repository.save(completed(id = "g-2", userId = "u-1", username = "alice"))
        repository.save(Fixtures.game(id = "g-3", userId = "u-2", username = "bob"))
    }

    @Test
    fun `lista apenas as partidas do proprio usuario`() {
        val page = listUseCase.list(UserId("u-1"), GameSearchQuery(page = 0, size = 10))

        assertEquals(2L, page.totalElements)
        assertEquals(setOf("g-1", "g-2"), page.items.map { it.id.value }.toSet())
    }

    @Test
    fun `filtra o historico por status`() {
        val completed = listUseCase.list(UserId("u-1"), GameSearchQuery(status = GameStatus.COMPLETED))
        assertEquals(listOf("g-2"), completed.items.map { it.id.value })

        val inProgress = listUseCase.list(UserId("u-1"), GameSearchQuery(status = GameStatus.IN_PROGRESS))
        assertEquals(listOf("g-1"), inProgress.items.map { it.id.value })
    }

    @Test
    fun `pagina o historico`() {
        val firstPage = listUseCase.list(UserId("u-1"), GameSearchQuery(page = 0, size = 1))
        assertEquals(1, firstPage.items.size)
        assertEquals(2L, firstPage.totalElements)

        val secondPage = listUseCase.list(UserId("u-1"), GameSearchQuery(page = 1, size = 1))
        assertEquals(1, secondPage.items.size)
        assertEquals(2L, secondPage.totalElements)
    }

    @Test
    fun `encontra a partida em andamento do usuario`() {
        assertEquals("g-1", inProgressUseCase.current(UserId("u-1"))?.id?.value)
        assertNull(inProgressUseCase.current(UserId("u-3")))
    }

    private fun completed(id: String, userId: String, username: String): Game =
        Game.reconstitute(
            Fixtures.game(id = id, userId = userId, username = username)
                .state()
                .copy(status = GameStatus.COMPLETED)
        )
}
