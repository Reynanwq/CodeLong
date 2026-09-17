package com.codelong.domain.service

import com.codelong.domain.valueobject.Difficulty
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random

class GameSequencerOrderingTest {

    private fun questionsOf(difficulty: Difficulty, count: Int, prefix: String = difficulty.name) =
        (1..count).map { Fixtures.question(id = "$prefix-$it", difficulty = difficulty) }

    @Test
    fun `lista vazia gera sequencia vazia`() {
        assertTrue(GameSequencer(Random(1)).sequence(emptyList()).isEmpty())
    }

    @Test
    fun `lista com apenas inativas gera sequencia vazia`() {
        val inactive = questionsOf(Difficulty.EASY, 3).map { it.deactivate(Fixtures.NOW) }

        assertTrue(GameSequencer(Random(1)).sequence(inactive).isEmpty())
    }

    @Test
    fun `pergunta unica e devolvida como snapshot`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.MEDIUM)

        val sequence = GameSequencer(Random(1)).sequence(listOf(question))

        assertEquals(1, sequence.size)
        assertEquals(question.id, sequence.first().id)
        assertEquals(question.difficulty, sequence.first().difficulty)
        assertEquals(question.options, sequence.first().options)
    }

    @Test
    fun `mantem ordem crescente com os dez niveis`() {
        val questions = Difficulty.entries.reversed().flatMap { questionsOf(it, 2) }

        val sequence = GameSequencer(Random(99)).sequence(questions)

        val levels = sequence.map { it.difficulty.level }
        assertEquals(levels.sorted(), levels)
        assertEquals(20, sequence.size)
    }

    @ParameterizedTest
    @EnumSource(Difficulty::class)
    fun `todas as perguntas do nivel aparecem juntas e nao vazias`(difficulty: Difficulty) {
        val questions = questionsOf(difficulty, 5)

        val sequence = GameSequencer(Random(5)).sequence(questions)

        assertEquals(5, sequence.size)
        assertTrue(sequence.all { it.difficulty == difficulty })
    }

    @Test
    fun `mesma semente produz a mesma ordem`() {
        val questions = questionsOf(Difficulty.HARD, 25)

        val first = GameSequencer(Random(1234)).sequence(questions).map { it.id }
        val second = GameSequencer(Random(1234)).sequence(questions).map { it.id }

        assertEquals(first, second)
    }

    @Test
    fun `sementes diferentes produzem ordens diferentes dentro do nivel`() {
        val questions = questionsOf(Difficulty.HARD, 25)

        val first = GameSequencer(Random(1)).sequence(questions).map { it.id }
        val second = GameSequencer(Random(2)).sequence(questions).map { it.id }

        assertEquals(first.toSet(), second.toSet())
        assertFalse(first == second)
    }

    @Test
    fun `nao embaralha globalmente entre niveis`() {
        val questions = listOf(
            Fixtures.question(id = "easy-1", difficulty = Difficulty.EASY),
            Fixtures.question(id = "easy-2", difficulty = Difficulty.EASY),
            Fixtures.question(id = "master-1", difficulty = Difficulty.MASTER),
            Fixtures.question(id = "master-2", difficulty = Difficulty.MASTER)
        )

        val sequence = GameSequencer(Random(77)).sequence(questions)

        assertEquals(
            listOf(Difficulty.EASY, Difficulty.EASY, Difficulty.MASTER, Difficulty.MASTER),
            sequence.map { it.difficulty }
        )
        assertTrue(sequence.take(2).all { it.id.value.startsWith("easy") })
        assertTrue(sequence.drop(2).all { it.id.value.startsWith("master") })
    }

    @Test
    fun `snapshot nao muda quando a pergunta original e editada depois`() {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.EASY)
        val sequence = GameSequencer(Random(1)).sequence(listOf(question))

        question.update(
            Fixtures.questionContent(statement = "Enunciado alterado", difficulty = Difficulty.MASTER),
            Fixtures.NOW.plusSeconds(10)
        )
        question.deactivate(Fixtures.NOW.plusSeconds(20))

        assertEquals(1, sequence.size)
        assertEquals("O que e polimorfismo?", sequence.first().statement)
        assertEquals(Difficulty.EASY, sequence.first().difficulty)
    }

    @Test
    fun `descarta inativas misturadas com ativas no mesmo nivel`() {
        val active = Fixtures.question(id = "ativa", difficulty = Difficulty.EASY)
        val inactive = Fixtures.question(id = "inativa", difficulty = Difficulty.EASY).deactivate(Fixtures.NOW)

        val sequence = GameSequencer(Random(1)).sequence(listOf(active, inactive))

        assertEquals(listOf(active.id), sequence.map { it.id })
    }

    @Test
    fun `preserva a quantidade total de perguntas ativas`() {
        val questions = Difficulty.entries.flatMap { questionsOf(it, 3) } +
            questionsOf(Difficulty.EASY, 4, prefix = "inativa").map { it.deactivate(Fixtures.NOW) }

        val sequence = GameSequencer(Random(3)).sequence(questions)

        assertEquals(30, sequence.size)
        assertEquals(30, sequence.map { it.id }.distinct().size)
    }
}
