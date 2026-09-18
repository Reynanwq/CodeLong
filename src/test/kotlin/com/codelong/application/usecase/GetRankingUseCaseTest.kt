package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryRankingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetRankingUseCaseTest {

    private val repository = InMemoryRankingRepository()
    private val useCase = GetRankingUseCaseImpl(repository)

    private fun add(
        id: String,
        username: String,
        score: Int,
        correctAnswers: Int = 10,
        totalTimeMillis: Long = 5000,
        achievedAtOffsetSeconds: Long = 0
    ) = repository.add(
        RankEntry(
            userId = UserId(id),
            username = username,
            score = score,
            correctAnswers = correctAnswers,
            answeredQuestions = 10,
            totalTimeMillis = totalTimeMillis,
            achievedAt = Fixtures.NOW.plusSeconds(achievedAtOffsetSeconds)
        )
    )

    @Test
    fun `ordena pela politica e atribui posicoes`() {
        add("u-1", "alice", score = 100, totalTimeMillis = 5000)
        add("u-2", "bob", score = 100, totalTimeMillis = 4000)
        add("u-3", "carol", score = 90)

        val page = useCase.ranking(0, 10)

        assertEquals(listOf("bob", "alice", "carol"), page.entries.map { it.username })
        assertEquals(listOf(1, 2, 3), page.entries.map { it.position })
        assertEquals(3L, page.totalElements)
    }

    @Test
    fun `lista todas as tentativas do mesmo usuario em linhas separadas`() {
        add("u-1", "alice", score = 50)
        add("u-1", "alice", score = 100)

        val page = useCase.ranking(0, 10)

        assertEquals(2, page.entries.size)
        assertEquals(listOf(100, 50), page.entries.map { it.score })
        assertEquals(2L, page.totalElements)
        assertEquals(listOf(1, 2), page.entries.map { it.position })
    }

    @Test
    fun `numeracao continua na proxima pagina`() {
        (1..5).forEach { add("u-$it", "user-$it", score = 100 - it) }

        val page = useCase.ranking(1, 2)

        assertEquals(2, page.entries.size)
        assertEquals(3, page.entries.first().position)
        assertEquals(4, page.entries.last().position)
    }

    @Test
    fun `valida parametros de paginacao`() {
        assertThrows<DomainException> { useCase.ranking(-1, 10) }
        assertThrows<DomainException> { useCase.ranking(0, 0) }
        assertThrows<DomainException> { useCase.ranking(0, 101) }
    }
}