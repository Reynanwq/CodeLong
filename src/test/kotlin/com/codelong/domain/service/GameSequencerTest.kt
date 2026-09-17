package com.codelong.domain.service

import com.codelong.domain.valueobject.Difficulty
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSequencerTest {

    @Test
    fun `ordena as perguntas por dificuldade crescente`() {
        val questions = listOf(
            Fixtures.question(id = "q-master", difficulty = Difficulty.MASTER),
            Fixtures.question(id = "q-easy", difficulty = Difficulty.EASY),
            Fixtures.question(id = "q-hard", difficulty = Difficulty.HARD),
            Fixtures.question(id = "q-medium", difficulty = Difficulty.MEDIUM)
        )

        val sequence = GameSequencer(Random(42)).sequence(questions)

        assertEquals(
            listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.MASTER),
            sequence.map { it.difficulty }
        )
    }

    @Test
    fun `inclui todas as perguntas exatamente uma vez`() {
        val questions = (1..10).map { index ->
            Fixtures.question(id = "q-$index", difficulty = Difficulty.fromLevel((index % 10) + 1))
        }

        val sequence = GameSequencer(Random(7)).sequence(questions)

        assertEquals(questions.size, sequence.size)
        assertEquals(questions.map { it.id }.toSet(), sequence.map { it.id }.toSet())
    }

    @Test
    fun `embaralha apenas dentro do mesmo nivel`() {
        val level = Difficulty.MEDIUM
        val questions = (1..20).map { index ->
            Fixtures.question(id = "q-$index", difficulty = level)
        }

        val sequence = GameSequencer(Random(123)).sequence(questions)

        assertTrue(sequence.all { it.difficulty == level })
        assertEquals(questions.map { it.id }.toSet(), sequence.map { it.id }.toSet())
    }

    @Test
    fun `ignora perguntas inativas`() {
        val active = Fixtures.question(id = "q-ativa", difficulty = Difficulty.EASY)
        val inactive = Fixtures.question(id = "q-inativa", difficulty = Difficulty.EASY).deactivate(Fixtures.NOW)

        val sequence = GameSequencer(Random(1)).sequence(listOf(active, inactive))

        assertEquals(listOf(active.id), sequence.map { it.id })
    }
}