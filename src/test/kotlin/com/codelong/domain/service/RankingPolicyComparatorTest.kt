package com.codelong.domain.service

import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RankingPolicyComparatorTest {

    private fun entry(
        id: String,
        score: Int = 100,
        correctAnswers: Int = 10,
        totalTimeMillis: Long = 5_000,
        achievedAtOffsetSeconds: Long = 0
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
    fun `entradas identicas sao equivalentes`() {
        val first = entry("a")
        val second = entry("a")

        assertEquals(0, RankingPolicy.comparator.compare(first, second))
        assertFalse(RankingPolicy.isBetter(first, second))
        assertFalse(RankingPolicy.isBetter(second, first))
    }

    @Test
    fun `isBetter e falso quando comparado consigo mesmo`() {
        val single = entry("a")

        assertFalse(RankingPolicy.isBetter(single, single))
    }

    @Test
    fun `comparator ordena do melhor para o pior`() {
        val worst = entry("worst", score = 10, correctAnswers = 1, totalTimeMillis = 9_000, achievedAtOffsetSeconds = 30)
        val best = entry("best", score = 200, correctAnswers = 20, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 5)
        val middle = entry("middle", score = 100, correctAnswers = 10, totalTimeMillis = 5_000, achievedAtOffsetSeconds = 10)

        val sorted = listOf(worst, best, middle).sortedWith(RankingPolicy.comparator)

        assertEquals(listOf("best", "middle", "worst"), sorted.map { it.username })
    }

    @Test
    fun `desempata pela cadeia completa de criterios`() {
        val champion = entry("champion", score = 100, correctAnswers = 10, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 0)
        val byScore = entry("by-score", score = 90, correctAnswers = 99, totalTimeMillis = 1, achievedAtOffsetSeconds = 0)
        val byTime = entry("by-time", score = 100, correctAnswers = 99, totalTimeMillis = 2_000, achievedAtOffsetSeconds = 0)
        val byCorrect = entry("by-correct", score = 100, correctAnswers = 9, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 0)
        val byDate = entry("by-date", score = 100, correctAnswers = 10, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 60)

        assertTrue(RankingPolicy.isBetter(champion, byScore))
        assertTrue(RankingPolicy.isBetter(champion, byTime))
        assertTrue(RankingPolicy.isBetter(champion, byCorrect))
        assertTrue(RankingPolicy.isBetter(champion, byDate))
    }

    @Test
    fun `pontuacao tem prioridade sobre todos os outros criterios`() {
        val highScoreSlow = entry("high", score = 101, correctAnswers = 0, totalTimeMillis = 999_999, achievedAtOffsetSeconds = 999)
        val lowScoreFast = entry("low", score = 100, correctAnswers = 100, totalTimeMillis = 1, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(highScoreSlow, lowScoreFast))
        assertFalse(RankingPolicy.isBetter(lowScoreFast, highScoreSlow))
    }

    @Test
    fun `tempo tem prioridade sobre acertos e data`() {
        val faster = entry("faster", score = 100, correctAnswers = 10, totalTimeMillis = 1, achievedAtOffsetSeconds = 999)
        val slowerMoreCorrect = entry("slower", score = 100, correctAnswers = 11, totalTimeMillis = 999_999, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(faster, slowerMoreCorrect))
        assertFalse(RankingPolicy.isBetter(slowerMoreCorrect, faster))
    }

    @Test
    fun `empate no tempo desempata por mais acertos`() {
        val moreCorrect = entry("more", score = 100, correctAnswers = 11, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 999)
        val lessCorrect = entry("less", score = 100, correctAnswers = 10, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(moreCorrect, lessCorrect))
    }

    @Test
    fun `tempo tem prioridade sobre a data`() {
        val fasterNewer = entry("faster", score = 100, correctAnswers = 10, totalTimeMillis = 1_000, achievedAtOffsetSeconds = 999)
        val slowerOlder = entry("slower", score = 100, correctAnswers = 10, totalTimeMillis = 2_000, achievedAtOffsetSeconds = 0)

        assertTrue(RankingPolicy.isBetter(fasterNewer, slowerOlder))
    }

    @Test
    fun `comparacao e antisimetrica`() {
        val first = entry("a", score = 100)
        val second = entry("b", score = 90)

        assertTrue(RankingPolicy.comparator.compare(first, second) < 0)
        assertTrue(RankingPolicy.comparator.compare(second, first) > 0)
    }

    @Test
    fun `comparacao e transitiva`() {
        val first = entry("a", score = 300)
        val second = entry("b", score = 200)
        val third = entry("c", score = 100)

        assertTrue(RankingPolicy.isBetter(first, second))
        assertTrue(RankingPolicy.isBetter(second, third))
        assertTrue(RankingPolicy.isBetter(first, third))
    }

    @Test
    fun `seleciona a melhor entrada por usuario`() {
        val entries = listOf(
            entry("a", score = 100, achievedAtOffsetSeconds = 0),
            entry("a", score = 250, achievedAtOffsetSeconds = 60),
            entry("a", score = 150, achievedAtOffsetSeconds = 120),
            entry("b", score = 300, achievedAtOffsetSeconds = 10),
            entry("b", score = 50, achievedAtOffsetSeconds = 20)
        )

        val ranked = entries
            .groupBy { it.userId }
            .map { (_, scores) -> scores.minWith(RankingPolicy.comparator) }
            .sortedWith(RankingPolicy.comparator)

        assertEquals(listOf("b", "a"), ranked.map { it.username })
        assertEquals(300, ranked.first().score)
        assertEquals(250, ranked.last().score)
    }

    @Test
    fun `atribui posicoes sequenciais ao ranking`() {
        val entries = listOf(
            entry("a", score = 300),
            entry("b", score = 200),
            entry("c", score = 100)
        )

        val ranked = entries.sortedWith(RankingPolicy.comparator)
            .mapIndexed { index, rankEntry -> rankEntry.copy(position = index + 1) }

        assertEquals(listOf(1, 2, 3), ranked.map { it.position })
        assertEquals(listOf("a", "b", "c"), ranked.map { it.username })
    }

    @Test
    fun `posicao nao influencia a ordenacao`() {
        val withoutPosition = entry("a", score = 100)
        val withPosition = entry("a", score = 100).copy(position = 7)

        assertEquals(0, RankingPolicy.comparator.compare(withoutPosition, withPosition))
    }

    @Test
    fun `scores negativos sao ordenados corretamente`() {
        val zero = entry("zero", score = 0)
        val negative = entry("negative", score = -10)

        assertTrue(RankingPolicy.isBetter(zero, negative))
    }
}
