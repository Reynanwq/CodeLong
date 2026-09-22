package com.codelong.domain.service

import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
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
        wrongAnswers = 0,
        mode = GameMode.CLASSIC,
        answeredQuestions = 10,
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
    fun `empate na pontuacao desempata pelo menor tempo`() {
        val faster = entry("a", score = 100, correctAnswers = 10, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 10)
        val slower = entry("b", score = 100, correctAnswers = 12, totalTimeMillis = 9_000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(faster, slower))
        assertFalse(RankingPolicy.isBetter(slower, faster))
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

    @Test
    fun `minimo de respostas e menor no modo genocida`() {
        assertEquals(10, RankingPolicy.minimumAnswers(GameMode.CLASSIC))
        assertEquals(5, RankingPolicy.minimumAnswers(GameMode.GENOCIDA))
        assertEquals(5, RankingPolicy.minimumAnswers(GameMode.APRENDIZADO))
    }

    @Test
    fun `elegibilidade respeita o minimo de cada modo`() {
        assertTrue(RankingPolicy.isEligible(entryWith(GameMode.CLASSIC, answered = 10)))
        assertFalse(RankingPolicy.isEligible(entryWith(GameMode.CLASSIC, answered = 9)))
        assertTrue(RankingPolicy.isEligible(entryWith(GameMode.GENOCIDA, answered = 5)))
        assertFalse(RankingPolicy.isEligible(entryWith(GameMode.GENOCIDA, answered = 4)))
    }

    private fun entryWith(mode: GameMode, answered: Int) = RankEntry(
        userId = UserId("u-1"),
        username = "alice",
        score = 100,
        correctAnswers = 3,
        wrongAnswers = 1,
        answeredQuestions = answered,
        mode = mode,
        totalTimeMillis = 1_000,
        achievedAt = Fixtures.NOW
    )
}