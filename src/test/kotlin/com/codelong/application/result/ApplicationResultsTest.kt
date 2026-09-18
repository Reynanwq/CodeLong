package com.codelong.application.result

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationResultsTest {

    @Test
    fun `AuthenticationResult carrega usuario e token`() {
        val user = Fixtures.user(id = "u-1", username = "alice")

        val result = AuthenticationResult(user = user, token = "jwt-token")

        assertEquals(user, result.user)
        assertEquals("jwt-token", result.token)
        assertEquals("alice", result.user.username.value)
    }

    @Test
    fun `AuthenticationResult tem igualdade por valor`() {
        val user = Fixtures.user(id = "u-1")
        val first = AuthenticationResult(user, "token")
        val second = AuthenticationResult(user, "token")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `GameCreationResult marca partida criada`() {
        val game = Fixtures.game(id = "g-1")

        val result = GameCreationResult(game = game, created = true)

        assertEquals(game, result.game)
        assertTrue(result.created)
    }

    @Test
    fun `GameCreationResult marca partida retomada`() {
        val game = Fixtures.game(id = "g-1")

        val result = GameCreationResult(game = game, created = false)

        assertFalse(result.created)
        assertEquals("g-1", result.game.id.value)
    }

    @Test
    fun `RankingPage carrega entradas e metadados`() {
        val entries = listOf(
            RankEntry(
                userId = UserId("u-1"),
                username = "alice",
                score = 100,
                correctAnswers = 3,
                answeredQuestions = 10,
                totalTimeMillis = 5_000,
                achievedAt = Fixtures.NOW,
                position = 1
            )
        )

        val page = RankingPage(entries = entries, totalElements = 1L, page = 0, size = 20)

        assertEquals(1, page.entries.size)
        assertEquals(1L, page.totalElements)
        assertEquals(0, page.page)
        assertEquals(20, page.size)
        assertEquals(1, page.entries.first().position)
    }

    @Test
    fun `RankingPage aceita lista vazia`() {
        val page = RankingPage(entries = emptyList(), totalElements = 0L, page = 0, size = 20)

        assertTrue(page.entries.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `RankEntry dentro do RankingPage preserva posicao opcional`() {
        val withoutPosition = RankEntry(
            userId = UserId("u-1"),
            username = "alice",
            score = 50,
            correctAnswers = 1,
            answeredQuestions = 10,
            totalTimeMillis = 1_000,
            achievedAt = Fixtures.NOW
        )

        val page = RankingPage(listOf(withoutPosition), 1L, 0, 20)

        assertNull(page.entries.first().position)
    }
}
