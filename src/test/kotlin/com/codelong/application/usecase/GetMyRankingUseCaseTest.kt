package com.codelong.application.usecase

import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.support.Fixtures
import com.codelong.support.InMemoryRankingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetMyRankingUseCaseTest {

    private lateinit var repository: InMemoryRankingRepository
    private lateinit var useCase: GetMyRankingUseCase

    private fun entry(
        id: String,
        score: Int,
        correctAnswers: Int = 1,
        totalTimeMillis: Long = 1_000,
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

    @BeforeEach
    fun setUp() {
        repository = InMemoryRankingRepository()
        useCase = GetMyRankingUseCaseImpl(repository)
    }

    @Test
    fun `retorna nulo quando o usuario nao concluiu partida`() {
        repository.add(entry("outro", score = 100))

        assertNull(useCase.myRanking(UserId("u-1")))
    }

    @Test
    fun `retorna nulo em ranking vazio`() {
        assertNull(useCase.myRanking(UserId("u-1")))
    }

    @Test
    fun `retorna posicao um quando ninguem e melhor`() {
        repository.add(entry("u-1", score = 500))

        val rank = useCase.myRanking(UserId("u-1"))

        assertEquals(1, rank?.position)
        assertEquals(500, rank?.score)
        assertEquals("u-1", rank?.username)
    }

    @Test
    fun `posicao considera quantos usuarios sao melhores`() {
        repository.add(entry("melhor-1", score = 500))
        repository.add(entry("melhor-2", score = 400))
        repository.add(entry("u-1", score = 300))

        assertEquals(3, useCase.myRanking(UserId("u-1"))?.position)
    }

    @Test
    fun `usa a melhor pontuacao do usuario`() {
        repository.add(entry("u-1", score = 100))
        repository.add(entry("u-1", score = 900))
        repository.add(entry("u-1", score = 400))

        val rank = useCase.myRanking(UserId("u-1"))

        assertEquals(900, rank?.score)
        assertEquals(1, rank?.position)
    }

    @Test
    fun `preserva os demais campos da entrada`() {
        repository.add(
            entry("u-1", score = 250, correctAnswers = 7, totalTimeMillis = 4_000, achievedAtOffsetSeconds = 30)
        )

        val rank = useCase.myRanking(UserId("u-1"))

        assertEquals(250, rank?.score)
        assertEquals(7, rank?.correctAnswers)
        assertEquals(4_000L, rank?.totalTimeMillis)
        assertEquals(Fixtures.NOW.plusSeconds(30), rank?.achievedAt)
    }

    @Test
    fun `nao altera a entrada armazenada`() {
        repository.add(entry("u-1", score = 100))

        useCase.myRanking(UserId("u-1"))

        assertEquals(null, repository.findUserBestScore(UserId("u-1"))?.position)
    }

    @Test
    fun `posicao e recalculada a cada chamada`() {
        repository.add(entry("u-1", score = 100))
        assertEquals(1, useCase.myRanking(UserId("u-1"))?.position)

        repository.add(entry("novo-lider", score = 999))
        assertEquals(2, useCase.myRanking(UserId("u-1"))?.position)
    }

    @Test
    fun `empate na pontuacao pode deixar o usuario em segunda posicao`() {
        repository.add(entry("u-1", score = 100, correctAnswers = 1))
        repository.add(entry("u-2", score = 100, correctAnswers = 5))

        assertEquals(2, useCase.myRanking(UserId("u-1"))?.position)
        assertEquals(1, useCase.myRanking(UserId("u-2"))?.position)
    }
}
