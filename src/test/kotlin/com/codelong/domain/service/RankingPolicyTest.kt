package com.codelong.domain.service

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RankingPolicyTest {

    private fun entry(
        id: String,
        score: Int,
        correctAnswers: Int,
        totalTimeMillis: Long,
        achievedAtOffsetSeconds: Long
    ) = RankEntry(
        userId = UserId(id),
        username = id,
        score = score,
        correctAnswers = correctAnswers,
        totalTimeMillis = totalTimeMillis,
        achievedAt = Fixtures.NOW.plusSeconds(achievedAtOffsetSeconds)
    )

    @Test
    fun `maior pontuacao vence`() {
        val better = entry("a", score = 100, correctAnswers = 1, totalTimeMillis = 9000, achievedAtOffsetSeconds = 10)
        val worse = entry("b", score = 90, correctAnswers = 9, totalTimeMillis = 1000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(better, worse))
        assertFalse(RankingPolicy.isBetter(worse, better))
    }

    @Test
    fun `empate na pontuacao desempata por mais acertos`() {
        val better = entry("a", score = 100, correctAnswers = 12, totalTimeMillis = 9000, achievedAtOffsetSeconds = 10)
        val worse = entry("b", score = 100, correctAnswers = 10, totalTimeMillis = 1000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(better, worse))
    }

    @Test
    fun `empate em acertos desempata pelo menor tempo`() {
        val faster = entry("a", score = 100, correctAnswers = 10, totalTimeMillis = 3000, achievedAtOffsetSeconds = 10)
        val slower = entry("b", score = 100, correctAnswers = 10, totalTimeMillis = 8000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(faster, slower))
    }

    @Test
    fun `empate no tempo desempata pela data mais antiga`() {
        val older = entry("a", score = 100, correctAnswers = 10, totalTimeMillis = 3000, achievedAtOffsetSeconds = 0)
        val newer = entry("b", score = 100, correctAnswers = 10, totalTimeMillis = 3000, achievedAtOffsetSeconds = 60)

        assertTrue(RankingPolicy.isBetter(older, newer))
    }
}