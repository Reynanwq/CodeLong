package com.codelong.integration

import com.codelong.domain.exception.DomainException

import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.QuestionSearch
import com.codelong.domain.GameRules
import com.codelong.domain.port.RankingFilter
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.QuestionDocument
import com.codelong.infrastructure.persistence.document.UserDocument
import com.codelong.support.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PersistenceIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun mongoProperties(registry: DynamicPropertyRegistry) = MongoTestEnvironment.register(registry)
    }

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var rankingRepository: RankingRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private fun filter(mode: GameMode? = null) = RankingFilter(mode, null)

    @BeforeEach
    fun cleanDatabase() {
        mongoTemplate.remove(Query(), UserDocument::class.java)
        mongoTemplate.remove(Query(), QuestionDocument::class.java)
        mongoTemplate.remove(Query(), GameDocument::class.java)
    }

    @Test
    fun `persiste e recupera usuario preservando o perfil`() {
        val saved = userRepository.save(Fixtures.user(id = "u-1", username = "user-1"))

        val found = userRepository.findById(UserId("u-1"))
        assertNotNull(found)
        assertEquals(saved.passwordHash, found!!.passwordHash)
        assertEquals("user-1", found.username.value)
        assertEquals("user-1@codelong.dev", found.email.value)
        assertTrue(found.isActive)
    }

    @Test
    fun `busca usuario por username e email`() {
        userRepository.save(Fixtures.user(id = "u-1", username = "alice"))

        assertNotNull(userRepository.findByUsername(Username.of("alice")))
        assertNotNull(userRepository.findByEmail(Email.of("alice@codelong.dev")))
        assertTrue(userRepository.existsByUsername(Username.of("alice")))
        assertTrue(userRepository.existsByEmail(Email.of("alice@codelong.dev")))
        assertFalse(userRepository.existsByUsername(Username.of("bob")))
        assertNull(userRepository.findByUsername(Username.of("bob")))
    }

    @Test
    fun `indice unico de username impede duplicidade`() {
        userRepository.save(Fixtures.user(id = "u-1", username = "alice"))

        assertThrows<DuplicateKeyException> {
            userRepository.save(Fixtures.user(id = "u-2", username = "alice"))
        }
    }

    @Test
    fun `busca de perguntas filtra por status categoria e dificuldade`() {
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY, category = Category.OOP))
        questionRepository.save(Fixtures.question(id = "q-2", difficulty = Difficulty.HARD, category = Category.KOTLIN))
        questionRepository.save(
            Fixtures.question(id = "q-3", difficulty = Difficulty.HARD, category = Category.KOTLIN)
                .deactivate(Fixtures.NOW)
        )

        assertEquals(2L, questionRepository.countActive())
        assertEquals(3, questionRepository.search(QuestionSearch(page = 0, size = 10)).totalElements)
        assertEquals(2, questionRepository.search(QuestionSearch(category = Category.KOTLIN)).totalElements)
        assertEquals(2, questionRepository.search(QuestionSearch(difficulty = Difficulty.HARD)).totalElements)
        assertEquals(2, questionRepository.search(QuestionSearch(status = QuestionStatus.ACTIVE)).totalElements)
        assertEquals(1, questionRepository.search(QuestionSearch(status = QuestionStatus.INACTIVE)).totalElements)
        assertEquals(2, questionRepository.findAllActive().size)

        val secondPage = questionRepository.search(QuestionSearch(page = 1, size = 2))
        assertEquals(1, secondPage.items.size)
        assertEquals(3L, secondPage.totalElements)
    }

    @Test
    fun `remove pergunta por id`() {
        questionRepository.save(Fixtures.question(id = "q-1"))

        questionRepository.deleteById(QuestionId("q-1"))

        assertNull(questionRepository.findById(QuestionId("q-1")))
        assertEquals(0, questionRepository.search(QuestionSearch()).totalElements)
    }

    @Test
    fun `persiste partida com snapshot das perguntas e respostas`() {
        val game = Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM))
        gameRepository.save(game)

        val loaded = gameRepository.findById(GameId("g-1"))
        assertNotNull(loaded)
        assertEquals(2, loaded!!.totalQuestions)
        assertEquals(UserId("u-1"), loaded.userId)
        assertEquals("user-1", loaded.username)
        assertTrue(loaded.isInProgress)
        assertEquals(0, loaded.answers.size)

        loaded.answer(loaded.currentQuestion().correctOption, Fixtures.NOW)
        gameRepository.save(loaded)

        val reloaded = gameRepository.findById(GameId("g-1"))!!
        assertEquals(1, reloaded.answers.size)
        assertEquals(1, reloaded.currentQuestionIndex)
        assertEquals(Difficulty.EASY.points, reloaded.score)
    }

    @Test
    fun `optimistic lock rejeita gravacao concorrente da mesma partida`() {
        val created = gameRepository.save(
            Fixtures.game(id = "g-1", userId = "u-1", difficulties = listOf(Difficulty.EASY, Difficulty.HARD))
        )

        val copyA = gameRepository.findById(GameId("g-1"))!!
        val copyB = gameRepository.findById(GameId("g-1"))!!

        copyA.answer(copyA.currentQuestion().correctOption, Fixtures.NOW)
        val savedA = gameRepository.save(copyA)
        assertTrue(savedA.version > created.version, "a versao deve avancar a cada gravacao")

        copyB.answer(copyB.currentQuestion().correctOption, Fixtures.NOW)
        assertThrows<DomainException> {
            gameRepository.save(copyB)
        }
    }

    @Test
    fun `ranking lista todas as tentativas, inclusive do mesmo usuario`() {
        persistCompletedGame(id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.EASY)
        persistCompletedGame(id = "g-2", userId = "u-1", username = "alice", difficulty = Difficulty.MASTER)
        persistCompletedGame(id = "g-3", userId = "u-2", username = "bob", difficulty = Difficulty.EASY)

        val ranking = rankingRepository.findRanking(0, 10, filter())

        assertEquals(3, ranking.size)
        assertEquals(listOf("alice", "alice", "bob"), ranking.map { it.username })
        assertEquals(Difficulty.MASTER.points * GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING, ranking.first().score)
        assertEquals(3L, rankingRepository.countRankedEntries(filter()))
    }

    @Test
    fun `ranking ignora partidas em andamento e calcula a posicao individual`() {
        persistCompletedGame(id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.MASTER)
        persistCompletedGame(id = "g-2", userId = "u-2", username = "bob", difficulty = Difficulty.EASY)
        gameRepository.save(Fixtures.game(id = "g-3", userId = "u-3", username = "carol"))

        val ranking = rankingRepository.findRanking(0, 10, filter())
        assertEquals(listOf("alice", "bob"), ranking.map { it.username })

        val bob = rankingRepository.findUserBestScore(UserId("u-2"), filter())!!
        assertEquals(1L, rankingRepository.countUsersBetterThan(bob, filter()))
        assertEquals(2L, rankingRepository.countRankedEntries(filter()))
    }

    @Test
    fun `ranking inclui partidas abandonadas com o minimo de respostas`() {
        persistAbandonedGame(id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.HARD)

        val ranking = rankingRepository.findRanking(0, 10, filter())

        assertEquals(1, ranking.size)
        assertEquals("alice", ranking.first().username)
        assertEquals(Difficulty.HARD.points * GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING, ranking.first().score)
        assertEquals(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING, ranking.first().answeredQuestions)
        assertEquals(1L, rankingRepository.countRankedEntries(filter()))
    }

    @Test
    fun `ranking ignora partida concluida abaixo do minimo de respostas`() {
        val belowMinimum = GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING - 1
        val game = Fixtures.game(
            id = "g-1",
            userId = "u-1",
            username = "alice",
            difficulties = List(belowMinimum) { Difficulty.EASY }
        )
        val loaded = gameRepository.save(game)
        repeat(belowMinimum) { loaded.answer(loaded.currentQuestion().correctOption, Fixtures.NOW) }
        gameRepository.save(loaded)

        assertTrue(rankingRepository.findRanking(0, 10, filter()).isEmpty())
        assertEquals(0L, rankingRepository.countRankedEntries(filter()))
    }

    @Test
    fun `ranking inclui partida derrotada no modo genocida`() {
        persistDefeatedGame(id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.EASY)

        val ranking = rankingRepository.findRanking(0, 10, filter())

        assertEquals(1, ranking.size)
        assertEquals("alice", ranking.first().username)
        assertEquals(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING, ranking.first().answeredQuestions)
        assertEquals(Difficulty.EASY.points * (GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING - 1), ranking.first().score)
    }

    @Test
    fun `ranking aceita genocida com o minimo menor e recusa abaixo dele`() {
        persistDefeatedGame(
            id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.EASY,
            correctAnswers = GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA - 1
        )
        persistDefeatedGame(
            id = "g-2", userId = "u-2", username = "bob", difficulty = Difficulty.EASY,
            correctAnswers = GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA - 2
        )

        val ranking = rankingRepository.findRanking(0, 10, filter())

        assertEquals(1, ranking.size)
        assertEquals("alice", ranking.first().username)
        assertEquals(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING_GENOCIDA, ranking.first().answeredQuestions)
    }

    @Test
    fun `ranking separa classico e genocida quando o modo e informado`() {
        persistCompletedGame(id = "g-1", userId = "u-1", username = "alice", difficulty = Difficulty.EASY)
        persistDefeatedGame(id = "g-2", userId = "u-2", username = "bob", difficulty = Difficulty.EASY)

        val classic = rankingRepository.findRanking(0, 10, filter(GameMode.CLASSIC))
        val genocida = rankingRepository.findRanking(0, 10, filter(GameMode.GENOCIDA))

        assertEquals(listOf("alice"), classic.map { it.username })
        assertEquals(listOf("bob"), genocida.map { it.username })
        assertEquals(1L, rankingRepository.countRankedEntries(filter(GameMode.CLASSIC)))
        assertEquals(1L, rankingRepository.countRankedEntries(filter(GameMode.GENOCIDA)))
        assertEquals(2L, rankingRepository.countRankedEntries(filter()))
        assertNull(rankingRepository.findUserBestScore(UserId("u-1"), filter(GameMode.GENOCIDA)))
        assertEquals("alice", rankingRepository.findUserBestScore(UserId("u-1"), filter(GameMode.CLASSIC))?.username)
    }

    private fun persistDefeatedGame(
        id: String,
        userId: String,
        username: String,
        difficulty: Difficulty,
        correctAnswers: Int = GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING - 1
    ) {
        val difficulties = List(correctAnswers + 1) { difficulty }
        val game = Fixtures.game(
            id = id,
            userId = userId,
            username = username,
            difficulties = difficulties,
            mode = com.codelong.domain.valueobject.GameMode.GENOCIDA
        )
        val loaded = gameRepository.save(game)
        repeat(correctAnswers) {
            loaded.answer(loaded.currentQuestion().correctOption, Fixtures.NOW)
        }
        val last = loaded.currentQuestion()
        loaded.answer(last.options.first { it.id != last.correctOption }.id, Fixtures.NOW)
        val defeated = gameRepository.save(loaded)
        assertEquals(com.codelong.domain.valueobject.GameStatus.DEFEATED, defeated.status)
    }

    private fun persistAbandonedGame(id: String, userId: String, username: String, difficulty: Difficulty) {
        val difficulties = List(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING + 5) { difficulty }
        val game = Fixtures.game(id = id, userId = userId, username = username, difficulties = difficulties)
        val loaded = gameRepository.save(game)
        repeat(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING) {
            loaded.answer(loaded.currentQuestion().correctOption, Fixtures.NOW)
        }
        loaded.abandon(Fixtures.NOW.plusSeconds(60))
        val abandoned = gameRepository.save(loaded)
        assertEquals(com.codelong.domain.valueobject.GameStatus.ABANDONED, abandoned.status)
    }

    private fun persistCompletedGame(id: String, userId: String, username: String, difficulty: Difficulty) {
        val difficulties = List(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING) { difficulty }
        val game = Fixtures.game(id = id, userId = userId, username = username, difficulties = difficulties)
        val loaded = gameRepository.save(game)
        repeat(difficulties.size) {
            loaded.answer(loaded.currentQuestion().correctOption, Fixtures.NOW)
        }
        val completed = gameRepository.save(loaded)
        assertEquals(com.codelong.domain.valueobject.GameStatus.COMPLETED, completed.status)
    }
}