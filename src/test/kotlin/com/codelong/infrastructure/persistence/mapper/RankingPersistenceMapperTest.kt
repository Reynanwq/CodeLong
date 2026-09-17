package com.codelong.infrastructure.persistence.mapper

import com.codelong.infrastructure.persistence.document.RankEntryDocument
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RankingPersistenceMapperTest {

    @Test
    fun `toDomain converte a projecao de ranking`() {
        val document = RankEntryDocument(
            userId = "u-1",
            username = "alice",
            score = 250,
            correctAnswers = 7,
            totalTimeMillis = 12_345,
            achievedAt = Fixtures.NOW
        )

        val entry = RankingPersistenceMapper.toDomain(document)

        assertEquals("u-1", entry.userId.value)
        assertEquals("alice", entry.username)
        assertEquals(250, entry.score)
        assertEquals(7, entry.correctAnswers)
        assertEquals(12_345L, entry.totalTimeMillis)
        assertEquals(Fixtures.NOW, entry.achievedAt)
    }

    @Test
    fun `toDomain nao define posicao`() {
        val entry = RankingPersistenceMapper.toDomain(RankEntryDocument(userId = "u-1"))

        assertNull(entry.position)
    }

    @Test
    fun `toDomain aceita projecao com valores padrao`() {
        val entry = RankingPersistenceMapper.toDomain(RankEntryDocument())

        assertEquals("", entry.userId.value)
        assertEquals("", entry.username)
        assertEquals(0, entry.score)
        assertEquals(0, entry.correctAnswers)
        assertEquals(0L, entry.totalTimeMillis)
    }
}
