package com.codelong.domain

import com.codelong.domain.model.Game
import com.codelong.domain.service.RankingPolicy
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameSetup
import com.codelong.domain.valueobject.Ids
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username
import com.codelong.support.Fixtures
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.AlphaChars
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.Size
import net.jqwik.api.constraints.StringLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant
import java.util.UUID

class DomainPropertiesTest {

    @Property
    fun `Difficulty fromLevel e inverso de level para todo nivel valido`(
        @ForAll @IntRange(min = 1, max = 10) level: Int
    ) {
        val difficulty = Difficulty.fromLevel(level)

        assertEquals(level, difficulty.level)
        assertEquals(level * 10, difficulty.points)
    }

    @Property
    fun `Difficulty orderedByLevel esta sempre ordenado`(@ForAll @IntRange(min = 1, max = 10) seed: Int) {
        val ordered = Difficulty.entries.shuffled(kotlin.random.Random(seed))

        assertEquals(Difficulty.orderedByLevel, ordered.sortedBy { it.level })
    }

    @Property
    fun `Email normaliza espacos e caixa alta quando valido`(
        @ForAll @AlphaChars @StringLength(min = 1, max = 20) local: String,
        @ForAll @AlphaChars @StringLength(min = 1, max = 10) domain: String,
        @ForAll @AlphaChars @StringLength(min = 2, max = 4) tld: String
    ) {
        val raw = "  ${local.uppercase()}@${domain.lowercase()}.${tld.lowercase()}  "

        assertTrue(Email.isValid(raw))
        assertEquals("${local.lowercase()}@${domain.lowercase()}.${tld.lowercase()}", Email.of(raw).value)
    }

    @Property
    fun `Email rejeita qualquer texto sem arroba`(
        @ForAll @StringLength(min = 0, max = 30) raw: String
    ) {
        if (!raw.contains('@')) {
            assertFalse(Email.isValid(raw))
        }
    }

    @Property
    fun `Username aceita qualquer valor alfanumerico de tamanho valido`(
        @ForAll @AlphaChars @StringLength(min = 3, max = 20) raw: String
    ) {
        assertTrue(Username.isValid(raw))
        assertEquals(raw, Username.of(raw).value)
    }

    @Property
    fun `Username rejeita valores curtos demais`(
        @ForAll @AlphaChars @StringLength(min = 0, max = 2) raw: String
    ) {
        assertFalse(Username.isValid(raw))
    }

    @Property
    fun `Username rejeita valores longos demais`(
        @ForAll @AlphaChars @StringLength(min = 21, max = 60) raw: String
    ) {
        assertFalse(Username.isValid(raw))
    }

    @Property
    fun `partida respondida corretamente soma exatamente os pontos das dificuldades`(
        @ForAll @Size(min = 1, max = 10) rawLevels: List<Int>
    ) {
        val difficulties = rawLevels.map { Difficulty.fromLevel(normalizeLevel(it)) }
        val questions = difficulties.mapIndexed { index, difficulty ->
            Fixtures.question(id = "q-$index", difficulty = difficulty).snapshot()
        }
        val game = Game.newGame(
            id = GameId("g-prop"),
            setup = GameSetup(UserId("u-1"), "alice", questions),
            startedAt = Fixtures.NOW
        )

        repeat(difficulties.size) {
            game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        }

        assertEquals(difficulties.sumOf { it.points }, game.score)
        assertEquals(difficulties.size, game.correctAnswers)
        assertEquals(0, game.wrongAnswers)
        assertTrue(game.isCompleted)
    }

    @Property
    fun `responder incorretamente nunca pontua`(
        @ForAll @Size(min = 1, max = 10) rawLevels: List<Int>
    ) {
        val levels = rawLevels.map { normalizeLevel(it) }
        val questions = levels.mapIndexed { index, level ->
            Fixtures.question(id = "q-$index", difficulty = Difficulty.fromLevel(level)).snapshot()
        }
        val game = Game.newGame(
            id = GameId("g-prop"),
            setup = GameSetup(UserId("u-1"), "alice", questions),
            startedAt = Fixtures.NOW
        )

        repeat(levels.size) {
            val question = game.currentQuestion()
            val wrong = question.options.first { it.id != question.correctOption }.id
            game.answer(wrong, Fixtures.NOW)
        }

        assertEquals(0, game.score)
        assertEquals(levels.size, game.wrongAnswers)
    }

    @Property
    fun `remainingQuestions nunca e negativo durante a partida`(
        @ForAll @Size(min = 1, max = 15) rawLevels: List<Int>
    ) {
        val levels = rawLevels.map { normalizeLevel(it) }
        val questions = levels.mapIndexed { index, level ->
            Fixtures.question(id = "q-$index", difficulty = Difficulty.fromLevel(level)).snapshot()
        }
        val game = Game.newGame(
            id = GameId("g-prop"),
            setup = GameSetup(UserId("u-1"), "alice", questions),
            startedAt = Fixtures.NOW
        )

        assertTrue(game.remainingQuestions >= 0)
        repeat(levels.size) {
            game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
            assertTrue(game.remainingQuestions >= 0)
        }
    }

    @Property
    fun `GameQuestion publicView nunca expoe a alternativa correta como campo`(
        @ForAll @IntRange(min = 1, max = 10) level: Int
    ) {
        val question = Fixtures.question(id = "q-1", difficulty = Difficulty.fromLevel(level))
        val publicView = question.snapshot().publicView()

        assertEquals(question.id, publicView.id)
        assertFalse(publicView.options.any { it.id.value.isBlank() })
    }

    @Property
    fun `RankingPolicy isBetter e antisimetrico`(
        @ForAll @IntRange(min = 0, max = 500) scoreA: Int,
        @ForAll @IntRange(min = 0, max = 50) correctA: Int,
        @ForAll @IntRange(min = 0, max = 100_000) timeA: Int,
        @ForAll @IntRange(min = 0, max = 500) scoreB: Int,
        @ForAll @IntRange(min = 0, max = 50) correctB: Int,
        @ForAll @IntRange(min = 0, max = 100_000) timeB: Int
    ) {
        val a = entry("a", scoreA, correctA, timeA.toLong())
        val b = entry("b", scoreB, correctB, timeB.toLong())

        if (RankingPolicy.isBetter(a, b)) {
            assertFalse(RankingPolicy.isBetter(b, a))
        }
    }

    @Property
    fun `RankingPolicy ordena por pontuacao decrescente`(
        @ForAll @Size(min = 2, max = 20) scores: List<@IntRange(min = 0, max = 1000) Int>
    ) {
        val entries = scores.mapIndexed { index, score ->
            entry("u-$index", score, 1, 1_000)
        }

        val sorted = entries.sortedWith(RankingPolicy.comparator)

        assertEquals(scores.sortedDescending(), sorted.map { it.score })
    }

    @Property
    fun `Ids newUserId sempre gera um UUID valido e unico`(
        @ForAll @IntRange(min = 1, max = 200) count: Int
    ) {
        val ids = (1..count).map { Ids.newUserId().value }

        assertEquals(count, ids.distinct().size)
        ids.forEach { assertEquals(it, UUID.fromString(it).toString()) }
    }

    @Property
    fun `opcoes de pergunta tem ids unicos e textos nao vazios`(
        @ForAll @IntRange(min = 2, max = 6) count: Int
    ) {
        val options = (0 until count).map { QuestionOption(com.codelong.domain.valueobject.OptionId("opt-$it"), "Texto $it") }

        assertEquals(count, options.map { it.id }.distinct().size)
        assertTrue(options.all { it.text.isNotBlank() })
    }

    @Property
    fun `reconstituir uma partida preserva pontuacao e progresso`(
        @ForAll @IntRange(min = 1, max = 8) answers: Int
    ) {
        val questions = (0..10).map { index ->
            Fixtures.question(id = "q-$index", difficulty = Difficulty.fromLevel((index % 10) + 1)).snapshot()
        }
        val game = Game.newGame(
            id = GameId("g-prop"),
            setup = GameSetup(UserId("u-1"), "alice", questions),
            startedAt = Fixtures.NOW
        )
        repeat(answers) {
            game.answer(game.currentQuestion().correctOption, Fixtures.NOW)
        }

        val restored = Game.reconstitute(game.state())

        assertEquals(game.score, restored.score)
        assertEquals(game.currentQuestionIndex, restored.currentQuestionIndex)
        assertEquals(game.status, restored.status)
        assertEquals(game.answers.size, restored.answers.size)
    }

    private fun normalizeLevel(value: Int): Int = Math.floorMod(value, 10) + 1

    private fun entry(id: String, score: Int, correctAnswers: Int, totalTimeMillis: Long) = RankEntry(
        userId = UserId(id),
        username = id,
        score = score,
        correctAnswers = correctAnswers,
        totalTimeMillis = totalTimeMillis,
        achievedAt = Instant.parse("2026-01-01T12:00:00Z")
    )

    @Property
    fun `QuestionId preserva qualquer conteudo recebido`(
        @ForAll @StringLength(min = 1, max = 50) raw: String
    ) {
        assertEquals(raw, QuestionId(raw).value)
    }
}
