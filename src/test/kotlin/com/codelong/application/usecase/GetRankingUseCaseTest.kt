package com.codelong.application.usecase

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.exception.DomainException

import com.codelong.domain.port.RankingFilter
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

    private fun filter(mode: GameMode? = null, theme: Category? = null) = RankingFilter(mode, theme)

    private fun add(
        id: String,
        username: String,
        score: Int,
        correctAnswers: Int = 10,
        totalTimeMillis: Long = 5000,
        achievedAtOffsetSeconds: Long = 0,
        mode: GameMode = GameMode.CLASSIC,
        theme: Category? = null
    ) = repository.add(
        RankEntry(
            userId = UserId(id),
            username = username,
            score = score,
            correctAnswers = correctAnswers,
            wrongAnswers = 0,
            mode = mode,
            answeredQuestions = 10,
            totalTimeMillis = totalTimeMillis,
            achievedAt = Fixtures.NOW.plusSeconds(achievedAtOffsetSeconds),
            theme = theme
        )
    )

    @Test
    fun `ordena pela politica e atribui posicoes`() {
        add("u-1", "alice", score = 100, totalTimeMillis = 5000)
        add("u-2", "bob", score = 100, totalTimeMillis = 4000)
        add("u-3", "carol", score = 90)

        val page = useCase.ranking(0, 10, filter())

        assertEquals(listOf("bob", "alice", "carol"), page.entries.map { it.username })
        assertEquals(listOf(1, 2, 3), page.entries.map { it.position })
        assertEquals(3L, page.totalElements)
    }

    @Test
    fun `lista todas as tentativas do mesmo usuario em linhas separadas`() {
        add("u-1", "alice", score = 50)
        add("u-1", "alice", score = 100)

        val page = useCase.ranking(0, 10, filter())

        assertEquals(2, page.entries.size)
        assertEquals(listOf(100, 50), page.entries.map { it.score })
        assertEquals(2L, page.totalElements)
        assertEquals(listOf(1, 2), page.entries.map { it.position })
    }

    @Test
    fun `numeracao continua na proxima pagina`() {
        (1..5).forEach { add("u-$it", "user-$it", score = 100 - it) }

        val page = useCase.ranking(1, 2, filter())

        assertEquals(2, page.entries.size)
        assertEquals(3, page.entries.first().position)
        assertEquals(4, page.entries.last().position)
    }

    @Test
    fun `valida parametros de paginacao`() {
        assertThrows<DomainException> { useCase.ranking(-1, 10, filter()) }
        assertThrows<DomainException> { useCase.ranking(0, 0, filter()) }
        assertThrows<DomainException> { useCase.ranking(0, 101, filter()) }
    }

    @Test
    fun `separa o ranking por modo`() {
        add("u-1", "alice", score = 100, mode = GameMode.CLASSIC)
        add("u-2", "bob", score = 200, mode = GameMode.GENOCIDA)
        add("u-3", "carol", score = 50, mode = GameMode.GENOCIDA)

        val classic = useCase.ranking(0, 10, filter(GameMode.CLASSIC))
        val genocida = useCase.ranking(0, 10, filter(GameMode.GENOCIDA))

        assertEquals(listOf("alice"), classic.entries.map { it.username })
        assertEquals(1L, classic.totalElements)
        assertEquals(listOf("bob", "carol"), genocida.entries.map { it.username })
        assertEquals(2L, genocida.totalElements)
        assertEquals(listOf(1, 2), genocida.entries.map { it.position })
    }

    @Test
    fun `sem modo informado mistura classico e genocida`() {
        add("u-1", "alice", score = 100, mode = GameMode.CLASSIC)
        add("u-2", "bob", score = 200, mode = GameMode.GENOCIDA)

        val page = useCase.ranking(0, 10, filter())

        assertEquals(listOf("bob", "alice"), page.entries.map { it.username })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `ranking de um modo vazio nao inclui o outro`() {
        add("u-1", "alice", score = 100, mode = GameMode.CLASSIC)

        val genocida = useCase.ranking(0, 10, filter(GameMode.GENOCIDA))

        assertEquals(0, genocida.entries.size)
        assertEquals(0L, genocida.totalElements)
    }

    @Test
    fun `ranking global ignora o modo aprendizado`() {
        add("u-1", "alice", score = 100, mode = GameMode.CLASSIC)
        add("u-2", "bob", score = 900, mode = GameMode.APRENDIZADO, theme = Category.KOTLIN)

        val global = useCase.ranking(0, 10, filter())

        assertEquals(listOf("alice"), global.entries.map { it.username })
        assertEquals(1L, global.totalElements)
    }

    @Test
    fun `ranking por tema filtra aprendizado pela categoria`() {
        add("u-1", "alice", score = 100, mode = GameMode.APRENDIZADO, theme = Category.KOTLIN)
        add("u-2", "bob", score = 200, mode = GameMode.APRENDIZADO, theme = Category.KOTLIN)
        add("u-3", "carol", score = 900, mode = GameMode.APRENDIZADO, theme = Category.REST)

        val kotlin = useCase.ranking(0, 10, filter(GameMode.APRENDIZADO, Category.KOTLIN))

        assertEquals(listOf("bob", "alice"), kotlin.entries.map { it.username })
        assertEquals(2L, kotlin.totalElements)
    }
}
