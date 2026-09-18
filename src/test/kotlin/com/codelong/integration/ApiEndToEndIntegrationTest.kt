package com.codelong.integration

import com.codelong.application.service.UserFactory
import com.codelong.domain.GameRules
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.Email
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ApiEndToEndIntegrationTest {

    companion object {
        private const val ADMIN_USERNAME = "admin"
        private const val ADMIN_PASSWORD = "admin12345"

        @JvmStatic
        @DynamicPropertySource
        fun mongoProperties(registry: DynamicPropertyRegistry) = MongoTestEnvironment.register(registry)
    }

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var userFactory: UserFactory

    @Value("\${local.server.port}")
    private var port: Int = 0

    private lateinit var api: HttpApiClient

    @BeforeEach
    fun setUp() {
        mongoTemplate.remove(Query(), UserDocument::class.java)
        mongoTemplate.remove(Query(), QuestionDocument::class.java)
        mongoTemplate.remove(Query(), GameDocument::class.java)
        userRepository.save(
            userFactory.createAdmin(
                Username.of(ADMIN_USERNAME),
                Email.of("admin@codelong.local"),
                ADMIN_PASSWORD
            )
        )
        api = HttpApiClient("http://localhost:$port")
    }

    @Test
    fun `registra autentica e consulta o proprio usuario`() {
        val registered = api.post("/api/auth/register", credentials("alice", "alice@codelong.dev", "secret123"))

        assertEquals(201, registered.status)
        assertEquals("alice", registered.json().obj("user").str("username"))
        assertEquals("USER", registered.json().obj("user").str("role"))
        val token = registered.json().str("token")
        assertTrue(token.isNotBlank())

        val login = api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "secret123"))
        assertEquals(200, login.status)
        assertTrue(login.json().str("token").isNotBlank())

        val me = api.get("/api/users/me", token)
        assertEquals(200, me.status)
        assertEquals("alice@codelong.dev", me.json().str("email"))
    }

    @Test
    fun `rejeita duplicidade credenciais invalidas e payload invalido`() {
        registerPlayer("alice")

        val duplicated = api.post("/api/auth/register", credentials("alice", "outro@codelong.dev", "secret123"))
        assertEquals(409, duplicated.status)
        assertEquals("USERNAME_ALREADY_EXISTS", duplicated.json().str("code"))

        val wrongPassword = api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "errada123"))
        assertEquals(401, wrongPassword.status)
        assertEquals("INVALID_CREDENTIALS", wrongPassword.json().str("code"))

        val invalidPayload = api.post("/api/auth/register", credentials("ab", "sem-arroba", "123"))
        assertEquals(400, invalidPayload.status)
        assertEquals("VALIDATION_ERROR", invalidPayload.json().str("code"))
    }

    @Test
    fun `protege endpoints e restringe a area administrativa`() {
        val playerToken = registerPlayer("alice")

        assertEquals(401, api.post("/api/games").status)
        assertEquals(401, api.get("/api/rankings").status)

        val forbidden = api.post("/api/admin/questions", questionBody("Pergunta", "EASY"), playerToken)
        assertEquals(403, forbidden.status)
    }

    @Test
    fun `jogador completa a partida e aparece no ranking`() {
        val adminToken = loginAdmin()
        val questions = (1..GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING).map { index ->
            createQuestion(adminToken, "Qual palavra-chave define constante em Kotlin? ($index)", "EASY")
        }
        val playerToken = registerPlayer("alice")

        val created = api.post("/api/games", token = playerToken)
        assertEquals(201, created.status)
        val gameId = created.json().str("id")
        assertEquals(questions.size, created.json().int("totalQuestions"))
        assertEquals("IN_PROGRESS", created.json().str("status"))

        val current = api.get("/api/games/$gameId/current-question", playerToken)
        assertEquals(200, current.status)
        assertTrue(questions.contains(current.json().str("id")))
        assertEquals(4, current.json().arr("options").size)
        assertFalse(current.json().containsKey("correctOption"))
        assertFalse(current.json().containsKey("explanation"))
        assertEquals(20, current.json().int("timeLimitSeconds"))

        repeat(questions.size) { index ->
            val response = api.post("/api/games/$gameId/answers", mapOf("optionId" to "a"), playerToken)
            assertEquals(200, response.status)
            assertTrue(response.json().bool("correct"))
            assertEquals(Difficulty.EASY.points, response.json().int("earnedPoints"))
            if (index == questions.size - 1) {
                assertTrue(response.json().bool("gameCompleted"))
                assertNull(response.json()["nextQuestion"])
            } else {
                assertFalse(response.json().bool("gameCompleted"))
                assertNotNull(response.json().obj("nextQuestion"))
            }
        }

        val expectedScore = Difficulty.EASY.points * questions.size
        val finished = api.get("/api/games/$gameId", playerToken)
        assertEquals("COMPLETED", finished.json().str("status"))
        assertEquals(expectedScore, finished.json().int("score"))
        assertEquals(questions.size, finished.json().int("correctAnswers"))
        assertEquals(0, finished.json().int("wrongAnswers"))

        val mine = api.get("/api/rankings/me", playerToken)
        assertEquals(200, mine.status)
        assertEquals(1, mine.json().int("position"))
        assertEquals(expectedScore, mine.json().int("score"))
        assertEquals(questions.size, mine.json().int("answeredQuestions"))

        val ranking = api.get("/api/rankings", playerToken)
        assertEquals(200, ranking.status)
        assertEquals(1L, ranking.json().long("totalElements"))
        assertEquals("alice", ranking.json().arr("entries").first().str("username"))
        assertEquals(questions.size, ranking.json().arr("entries").first().int("answeredQuestions"))

        val classic = api.get("/api/rankings?mode=CLASSIC", playerToken)
        assertEquals(200, classic.status)
        assertEquals(1L, classic.json().long("totalElements"))
        assertEquals("alice", classic.json().arr("entries").first().str("username"))

        val genocida = api.get("/api/rankings?mode=GENOCIDA", playerToken)
        assertEquals(200, genocida.status)
        assertEquals(0L, genocida.json().long("totalElements"))

        val mineGenocida = api.get("/api/rankings/me?mode=GENOCIDA", playerToken)
        assertEquals(204, mineGenocida.status)

        val invalidMode = api.get("/api/rankings?mode=INVALIDO", playerToken)
        assertEquals(400, invalidMode.status)
        assertEquals("game.mode.invalid", invalidMode.json().str("code"))
    }

    @Test
    fun `ranking individual responde 204 quando o jogador ainda nao concluiu partida`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")

        assertEquals(201, api.post("/api/games", token = playerToken).status)

        assertEquals(204, api.get("/api/rankings/me", playerToken).status)
    }

    @Test
    fun `historico de partidas e retomada da partida em andamento`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")

        val created = api.post("/api/games", token = playerToken)
        assertEquals(201, created.status)
        val gameId = created.json().str("id")

        val resumed = api.post("/api/games", token = playerToken)
        assertEquals(200, resumed.status)
        assertEquals(gameId, resumed.json().str("id"))

        val inProgress = api.get("/api/games/in-progress", playerToken)
        assertEquals(200, inProgress.status)
        assertEquals(gameId, inProgress.json().str("id"))

        val history = api.get("/api/games?page=0&size=10", playerToken)
        assertEquals(200, history.status)
        assertEquals(1L, history.json().long("totalElements"))
        assertEquals(gameId, history.json().arr("items").first().str("id"))

        val inProgressOnly = api.get("/api/games?status=IN_PROGRESS", playerToken)
        assertEquals(1L, inProgressOnly.json().long("totalElements"))

        val completedOnly = api.get("/api/games?status=COMPLETED", playerToken)
        assertEquals(0L, completedOnly.json().long("totalElements"))

        assertEquals(200, api.post("/api/games/$gameId/abandon", token = playerToken).status)
        assertEquals(204, api.get("/api/games/in-progress", playerToken).status)

        val newGame = api.post("/api/games", token = playerToken)
        assertEquals(201, newGame.status)
        assertTrue(newGame.json().str("id") != gameId)

        val abandoned = api.get("/api/games?status=ABANDONED", playerToken)
        assertEquals(1L, abandoned.json().long("totalElements"))
        assertEquals(2L, api.get("/api/games", playerToken).json().long("totalElements"))
    }

    @Test
    fun `abandonar encerra a partida e bloqueia novas respostas`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")
        val gameId = api.post("/api/games", token = playerToken).json().str("id")

        val abandoned = api.post("/api/games/$gameId/abandon", token = playerToken)
        assertEquals(200, abandoned.status)
        assertEquals("ABANDONED", abandoned.json().str("status"))

        val answer = api.post("/api/games/$gameId/answers", mapOf("optionId" to "opt-0"), playerToken)
        assertEquals(409, answer.status)
        assertEquals("GAME_FINISHED", answer.json().str("code"))
    }

    @Test
    fun `resposta invalida e acesso indevido sao rejeitados`() {
        seedQuestion()
        val ownerToken = registerPlayer("alice")
        val intruderToken = registerPlayer("bob")
        val gameId = api.post("/api/games", token = ownerToken).json().str("id")

        val invalid = api.post("/api/games/$gameId/answers", mapOf("optionId" to "nao-existe"), ownerToken)
        assertEquals(400, invalid.status)
        assertEquals("answer.option.invalid", invalid.json().str("code"))

        val forbidden = api.post("/api/games/$gameId/answers", mapOf("optionId" to "opt-0"), intruderToken)
        assertEquals(403, forbidden.status)

        val notFound = api.get("/api/games/inexistente", ownerToken)
        assertEquals(404, notFound.status)
        assertEquals("GAME_NOT_FOUND", notFound.json().str("code"))
    }

    @Test
    fun `nao inicia partida sem perguntas ativas`() {
        val playerToken = registerPlayer("alice")

        val response = api.post("/api/games", token = playerToken)

        assertEquals(409, response.status)
        assertEquals("NO_ACTIVE_QUESTIONS", response.json().str("code"))
    }

    @Test
    fun `admin gerencia o ciclo de vida da pergunta`() {
        val adminToken = loginAdmin()
        val questionId = createQuestion(adminToken, "Pergunta administravel", "MEDIUM")

        val deactivated = api.patch("/api/admin/questions/$questionId/status", mapOf("active" to false), adminToken)
        assertEquals(200, deactivated.status)
        assertEquals("INACTIVE", deactivated.json().str("status"))
        assertEquals(0L, questionRepository.countActive())

        val reactivated = api.patch("/api/admin/questions/$questionId/status", mapOf("active" to true), adminToken)
        assertEquals(200, reactivated.status)
        assertEquals("ACTIVE", reactivated.json().str("status"))

        val fetched = api.get("/api/admin/questions/$questionId", adminToken)
        assertEquals(200, fetched.status)
        assertEquals("MEDIUM", fetched.json().str("difficulty"))

        val listed = api.get("/api/admin/questions?page=0&size=10", adminToken)
        assertEquals(200, listed.status)
        assertEquals(1L, listed.json().long("totalElements"))

        assertEquals(204, api.delete("/api/admin/questions/$questionId", adminToken).status)
        assertEquals(404, api.get("/api/admin/questions/$questionId", adminToken).status)
    }

    @Test
    fun `usuario troca a propria senha`() {
        val token = registerPlayer("alice")

        val changed = api.patch(
            "/api/users/me/password",
            mapOf("currentPassword" to "secret123", "newPassword" to "novasenha123"),
            token
        )
        assertEquals(204, changed.status)

        val oldPassword = api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "secret123"))
        assertEquals(401, oldPassword.status)

        val newPassword = api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "novasenha123"))
        assertEquals(200, newPassword.status)

        val wrongCurrent = api.patch(
            "/api/users/me/password",
            mapOf("currentPassword" to "errada123", "newPassword" to "outrasenha123"),
            token
        )
        assertEquals(401, wrongCurrent.status)
        assertEquals("INVALID_CURRENT_PASSWORD", wrongCurrent.json().str("code"))

        val weak = api.patch(
            "/api/users/me/password",
            mapOf("currentPassword" to "novasenha123", "newPassword" to "curta"),
            token
        )
        assertEquals(400, weak.status)

        assertEquals(401, api.patch("/api/users/me/password", mapOf("currentPassword" to "a", "newPassword" to "outrasenha123")).status)
    }

    @Test
    fun `admin gerencia o ciclo de vida do usuario`() {
        val adminToken = loginAdmin()
        registerPlayer("alice")

        val listed = api.get("/api/admin/users?page=0&size=10", adminToken)
        assertEquals(200, listed.status)
        assertEquals(2L, listed.json().long("totalElements"))

        val aliceId = listed.json().arr("items").first { it.str("username") == "alice" }.str("id")

        val deactivated = api.patch("/api/admin/users/$aliceId/status", mapOf("active" to false), adminToken)
        assertEquals(200, deactivated.status)
        assertEquals("INACTIVE", deactivated.json().str("status"))

        val blocked = api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "secret123"))
        assertEquals(401, blocked.status)
        assertEquals("ACCOUNT_INACTIVE", blocked.json().str("code"))

        val byStatus = api.get("/api/admin/users?status=INACTIVE", adminToken)
        assertEquals(1L, byStatus.json().long("totalElements"))

        val reactivated = api.patch("/api/admin/users/$aliceId/status", mapOf("active" to true), adminToken)
        assertEquals(200, reactivated.status)
        assertEquals("ACTIVE", reactivated.json().str("status"))

        assertEquals(
            200,
            api.post("/api/auth/login", mapOf("identifier" to "alice", "password" to "secret123")).status
        )

        val adminId = api.get("/api/users/me", adminToken).json().str("id")
        val self = api.patch("/api/admin/users/$adminId/status", mapOf("active" to false), adminToken)
        assertEquals(400, self.status)
        assertEquals("user.deactivate.self", self.json().str("code"))
    }

    @Test
    fun `admin atualiza pergunta e filtra as listagens administrativas`() {
        val adminToken = loginAdmin()
        registerPlayer("alice")
        val questionId = createQuestion(adminToken, "Pergunta original", "EASY")

        val updated = api.put(
            "/api/admin/questions/$questionId",
            questionBody("Pergunta atualizada", "MASTER"),
            adminToken
        )
        assertEquals(200, updated.status)
        assertEquals(questionId, updated.json().str("id"))
        assertEquals("Pergunta atualizada", updated.json().str("statement"))
        assertEquals("MASTER", updated.json().str("difficulty"))
        assertEquals("ACTIVE", updated.json().str("status"))

        val unfiltered = api.get("/api/admin/questions", adminToken)
        assertEquals(1L, unfiltered.json().long("totalElements"))

        val byStatus = api.get("/api/admin/questions?status=active", adminToken)
        assertEquals(1L, byStatus.json().long("totalElements"))

        val byCategory = api.get("/api/admin/questions?category=kotlin", adminToken)
        assertEquals(1L, byCategory.json().long("totalElements"))

        val byDifficulty = api.get("/api/admin/questions?difficulty=master", adminToken)
        assertEquals(1L, byDifficulty.json().long("totalElements"))

        val combined = api.get(
            "/api/admin/questions?status=ACTIVE&category=KOTLIN&difficulty=MASTER&page=0&size=5",
            adminToken
        )
        assertEquals(1L, combined.json().long("totalElements"))

        val none = api.get("/api/admin/questions?category=KAFKA", adminToken)
        assertEquals(0L, none.json().long("totalElements"))

        val admins = api.get("/api/admin/users?role=ADMIN", adminToken)
        assertEquals(1L, admins.json().long("totalElements"))

        val players = api.get("/api/admin/users?role=USER", adminToken)
        assertEquals(1L, players.json().long("totalElements"))

        val activeAdmins = api.get("/api/admin/users?status=ACTIVE&role=ADMIN", adminToken)
        assertEquals(1L, activeAdmins.json().long("totalElements"))
    }


    @Test
    fun `no modo genocida errar encerra a partida como derrota`() {
        val adminToken = loginAdmin()
        createQuestion(adminToken, "Pergunta um?", "EASY")
        createQuestion(adminToken, "Pergunta dois?", "EASY")
        val playerToken = registerPlayer("alice")
        val gameId = api.post("/api/games?mode=GENOCIDA", token = playerToken).json().str("id")

        val wrong = api.post("/api/games/$gameId/answers", mapOf("optionId" to "b"), playerToken)

        assertEquals(200, wrong.status)
        assertFalse(wrong.json().bool("correct"))
        assertTrue(wrong.json().bool("gameCompleted"))
        assertEquals("DEFEATED", wrong.json().str("status"))
        assertNull(wrong.json()["nextQuestion"])

        val game = api.get("/api/games/$gameId", playerToken)
        assertEquals("DEFEATED", game.json().str("status"))
        assertEquals(1, game.json().int("wrongAnswers"))
        assertEquals(0, game.json().int("score"))
    }

    @Test
    fun `no modo genocida acertar mantem a partida em andamento`() {
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))
        questionRepository.save(Fixtures.question(id = "q-2", difficulty = Difficulty.HARD))
        val playerToken = registerPlayer("alice")
        val gameId = api.post("/api/games?mode=GENOCIDA", token = playerToken).json().str("id")

        val answer = api.post("/api/games/$gameId/answers", mapOf("optionId" to "opt-0"), playerToken)

        assertEquals(200, answer.status)
        assertTrue(answer.json().bool("correct"))
        assertFalse(answer.json().bool("gameCompleted"))
        assertEquals("IN_PROGRESS", answer.json().str("status"))
    }

    @Test
    fun `inicia partida no modo genocida e recusa modo invalido`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")

        val created = api.post("/api/games?mode=GENOCIDA", token = playerToken)
        assertEquals(201, created.status)
        assertEquals("GENOCIDA", created.json().str("mode"))

        val inProgress = api.get("/api/games/in-progress", playerToken)
        assertEquals(200, inProgress.status)
        assertEquals("GENOCIDA", inProgress.json().str("mode"))

        val invalid = api.post("/api/games?mode=INVALIDO", token = playerToken)
        assertEquals(400, invalid.status)
        assertEquals("game.mode.invalid", invalid.json().str("code"))
    }

    @Test
    fun `partida sem modo informado usa o classico`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")

        val created = api.post("/api/games", token = playerToken)

        assertEquals(201, created.status)
        assertEquals("CLASSIC", created.json().str("mode"))
    }

    @Test
    fun `pergunta sem resposta dentro do prazo conta como erro e a partida avanca`() {
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))
        questionRepository.save(Fixtures.question(id = "q-2", difficulty = Difficulty.HARD))
        val playerToken = registerPlayer("alice")
        val gameId = api.post("/api/games", token = playerToken).json().str("id")

        val current = api.get("/api/games/$gameId/current-question", playerToken)
        assertEquals(200, current.status)
        assertEquals(20, current.json().int("timeLimitSeconds"))
        assertTrue(current.json().containsKey("deadline"))

        expireCurrentQuestion(gameId)

        val late = api.post("/api/games/$gameId/answers", mapOf("optionId" to "opt-0"), playerToken)
        assertEquals(409, late.status)
        assertEquals("ANSWER_TIME_EXPIRED", late.json().str("code"))

        val next = api.get("/api/games/$gameId/current-question", playerToken)
        assertEquals(200, next.status)
        assertTrue(next.json().str("id") != current.json().str("id"))

        val game = api.get("/api/games/$gameId", playerToken)
        assertEquals(1, game.json().int("currentQuestionIndex"))
        assertEquals(1, game.json().int("wrongAnswers"))
        assertEquals(0, game.json().int("correctAnswers"))
        assertEquals(0, game.json().int("score"))

        val answered = api.post("/api/games/$gameId/answers", mapOf("optionId" to "opt-0"), playerToken)
        assertEquals(200, answered.status)
        assertTrue(answered.json().bool("gameCompleted"))
        assertTrue(answered.json().bool("correct"))
    }

    private fun expireCurrentQuestion(gameId: String) {
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").`is`(gameId)),
            Update().set("currentQuestionDeadline", java.time.Instant.now().minusSeconds(60)),
            GameDocument::class.java
        )
    }

    private fun credentials(username: String, email: String, password: String): Map<String, String> =
        mapOf("username" to username, "email" to email, "password" to password)

    private fun registerPlayer(username: String): String {
        val response = api.post(
            "/api/auth/register",
            credentials(username, "$username@codelong.dev", "secret123")
        )
        assertEquals(201, response.status)
        return response.json().str("token")
    }

    private fun loginAdmin(): String {
        val response = api.post(
            "/api/auth/login",
            mapOf("identifier" to ADMIN_USERNAME, "password" to ADMIN_PASSWORD)
        )
        assertEquals(200, response.status)
        return response.json().str("token")
    }

    private fun createQuestion(adminToken: String, statement: String, difficulty: String): String {
        val response = api.post("/api/admin/questions", questionBody(statement, difficulty), adminToken)
        assertEquals(201, response.status)
        return response.json().str("id")
    }

    private fun questionBody(statement: String, difficulty: String): Map<String, Any> = mapOf(
        "statement" to statement,
        "options" to listOf(
            mapOf("id" to "a", "text" to "Alternativa correta"),
            mapOf("id" to "b", "text" to "Alternativa incorreta"),
            mapOf("id" to "c", "text" to "Outra alternativa"),
            mapOf("id" to "d", "text" to "Mais uma alternativa")
        ),
        "correctOption" to "a",
        "explanation" to "Explicacao da alternativa correta.",
        "category" to "KOTLIN",
        "difficulty" to difficulty
    )

    private fun seedQuestion() {
        questionRepository.save(Fixtures.question(id = "q-1", difficulty = Difficulty.EASY))
    }
}