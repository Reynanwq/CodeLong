package com.codelong.integration

import com.codelong.application.service.UserFactory
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
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
        val easyQuestionId = createQuestion(adminToken, "Qual palavra-chave define constante em Kotlin?", "EASY")
        val hardQuestionId = createQuestion(adminToken, "Qual padrao delega a criacao as subclasses?", "HARD")
        val playerToken = registerPlayer("alice")

        val created = api.post("/api/games", token = playerToken)
        assertEquals(201, created.status)
        val gameId = created.json().str("id")
        assertEquals(2, created.json().int("totalQuestions"))
        assertEquals("IN_PROGRESS", created.json().str("status"))

        val current = api.get("/api/games/$gameId/current-question", playerToken)
        assertEquals(200, current.status)
        assertEquals(easyQuestionId, current.json().str("id"))
        assertEquals(4, current.json().arr("options").size)
        assertFalse(current.json().containsKey("correctOption"))
        assertFalse(current.json().containsKey("explanation"))

        val first = api.post("/api/games/$gameId/answers", mapOf("optionId" to "a"), playerToken)
        assertEquals(200, first.status)
        assertTrue(first.json().bool("correct"))
        assertEquals(Difficulty.EASY.points, first.json().int("earnedPoints"))
        assertFalse(first.json().bool("gameCompleted"))
        assertEquals(hardQuestionId, first.json().obj("nextQuestion").str("id"))

        val second = api.post("/api/games/$gameId/answers", mapOf("optionId" to "a"), playerToken)
        assertEquals(200, second.status)
        assertTrue(second.json().bool("gameCompleted"))
        assertEquals(Difficulty.EASY.points + Difficulty.HARD.points, second.json().int("currentScore"))
        assertNull(second.json()["nextQuestion"])

        val finished = api.get("/api/games/$gameId", playerToken)
        assertEquals("COMPLETED", finished.json().str("status"))

        val mine = api.get("/api/rankings/me", playerToken)
        assertEquals(200, mine.status)
        assertEquals(1, mine.json().int("position"))
        assertEquals(Difficulty.EASY.points + Difficulty.HARD.points, mine.json().int("score"))

        val ranking = api.get("/api/rankings", playerToken)
        assertEquals(200, ranking.status)
        assertEquals(1L, ranking.json().long("totalElements"))
        assertEquals("alice", ranking.json().arr("entries").first().str("username"))
    }

    @Test
    fun `ranking individual responde 204 quando o jogador ainda nao concluiu partida`() {
        seedQuestion()
        val playerToken = registerPlayer("alice")

        assertEquals(201, api.post("/api/games", token = playerToken).status)

        assertEquals(204, api.get("/api/rankings/me", playerToken).status)
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