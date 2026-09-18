package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.RankEntryDocument
import com.codelong.infrastructure.persistence.mapper.RankingPersistenceMapper
import com.codelong.support.Fixtures
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults

class MongoRankingRepositoryAdapterTest {

    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: MongoRankingRepositoryAdapter

    private fun document(
        id: String,
        score: Int = 100,
        correctAnswers: Int = 3,
        totalTimeMillis: Long = 5_000,
        achievedAtOffsetSeconds: Long = 0
    ) = RankEntryDocument(
        userId = id,
        username = id,
        score = score,
        correctAnswers = correctAnswers,
        wrongAnswers = 0,
        mode = "CLASSIC",
        answeredQuestions = 10,
        totalTimeMillis = totalTimeMillis,
        achievedAt = Fixtures.NOW.plusSeconds(achievedAtOffsetSeconds)
    )

    private fun entry(
        id: String,
        score: Int = 100,
        correctAnswers: Int = 3,
        totalTimeMillis: Long = 5_000,
        achievedAtOffsetSeconds: Long = 0
    ) = RankingPersistenceMapper.toDomain(
        document(id, score, correctAnswers, totalTimeMillis, achievedAtOffsetSeconds)
    )

    private fun stubAggregation(entries: List<RankEntryDocument>) {
        `when`(
            mongoTemplate.aggregate(
                any(Aggregation::class.java),
                eq(GameDocument::class.java),
                eq(RankEntryDocument::class.java)
            )
        ).thenReturn(AggregationResults(entries, Document()))
    }

    @BeforeEach
    fun setUp() {
        mongoTemplate = mock(MongoTemplate::class.java)
        adapter = MongoRankingRepositoryAdapter(mongoTemplate)
    }

    @Test
    fun `findRanking devolve as entradas mapeadas`() {
        stubAggregation(listOf(document("a", score = 300), document("b", score = 200)))

        val ranking = adapter.findRanking(page = 0, size = 10)

        assertEquals(2, ranking.size)
        assertEquals("a", ranking.first().userId.value)
        assertEquals(300, ranking.first().score)
        assertEquals(Fixtures.NOW, ranking.first().achievedAt)
    }

    @Test
    fun `findRanking aplica paginacao`() {
        stubAggregation((1..5).map { document("u-$it", score = 100 - it) })

        val firstPage = adapter.findRanking(page = 0, size = 2)
        val secondPage = adapter.findRanking(page = 1, size = 2)

        assertEquals(listOf("u-1", "u-2"), firstPage.map { it.userId.value })
        assertEquals(listOf("u-3", "u-4"), secondPage.map { it.userId.value })
    }

    @Test
    fun `findRanking devolve lista vazia quando nao ha partidas concluidas`() {
        stubAggregation(emptyList())

        assertEquals(0, adapter.findRanking(page = 0, size = 10).size)
    }

    @Test
    fun `findRanking alem do total devolve lista vazia`() {
        stubAggregation(listOf(document("a")))

        assertEquals(0, adapter.findRanking(page = 5, size = 10).size)
    }

    @Test
    fun `findUserBestScore devolve a entrada do usuario`() {
        stubAggregation(listOf(document("a", score = 300), document("b", score = 200)))

        val entry = adapter.findUserBestScore(UserId("b"))

        assertEquals("b", entry?.userId?.value)
        assertEquals(200, entry?.score)
    }

    @Test
    fun `findUserBestScore devolve nulo quando o usuario nao tem pontuacao`() {
        stubAggregation(listOf(document("a")))

        assertNull(adapter.findUserBestScore(UserId("inexistente")))
    }

    @Test
    fun `countUsersBetterThan conta apenas os melhores`() {
        stubAggregation(
            listOf(
                document("melhor", score = 300),
                document("meio", score = 200),
                document("pior", score = 100)
            )
        )

        val meia = document("meio", score = 200)

        assertEquals(1L, adapter.countUsersBetterThan(entry("meio", score = 200)))
    }

    @Test
    fun `countUsersBetterThan e zero para o lider`() {
        stubAggregation(listOf(document("lider", score = 500), document("outro", score = 100)))

        assertEquals(0L, adapter.countUsersBetterThan(entry("lider", score = 500)))
    }

    @Test
    fun `countUsersBetterThan considera todos quando o usuario e o pior`() {
        stubAggregation(listOf(document("a", score = 300), document("b", score = 200)))

        assertEquals(2L, adapter.countUsersBetterThan(entry("c", score = 10)))
    }

    @Test
    fun `countRankedUsers conta as entradas`() {
        stubAggregation((1..4).map { document("u-$it") })

        assertEquals(4L, adapter.countRankedEntries())
    }

    @Test
    fun `countRankedUsers e zero sem partidas concluidas`() {
        stubAggregation(emptyList())

        assertEquals(0L, adapter.countRankedEntries())
    }

    @Test
    fun `desempate por acertos e respeitado na contagem`() {
        stubAggregation(
            listOf(
                document("mais-acertos", score = 100, correctAnswers = 5),
                document("menos-acertos", score = 100, correctAnswers = 2)
            )
        )

        assertEquals(1L, adapter.countUsersBetterThan(entry("menos-acertos", score = 100, correctAnswers = 2)))
    }

    @Test
    fun `desempate por tempo e respeitado na contagem`() {
        stubAggregation(
            listOf(
                document("mais-rapido", score = 100, correctAnswers = 3, totalTimeMillis = 1_000),
                document("mais-lento", score = 100, correctAnswers = 3, totalTimeMillis = 9_000)
            )
        )

        assertEquals(1L, adapter.countUsersBetterThan(entry("mais-lento", score = 100, correctAnswers = 3, totalTimeMillis = 9_000)))
    }
}
