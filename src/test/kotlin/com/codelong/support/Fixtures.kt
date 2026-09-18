package com.codelong.support

import com.codelong.domain.valueobject.GameMode

import com.codelong.domain.model.Game
import com.codelong.domain.model.Question
import com.codelong.domain.model.User
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameSetup
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.QuestionContent
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.UserProfile
import com.codelong.domain.valueobject.Username
import java.time.Instant

object Fixtures {

    val NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")

    fun questionContent(
        statement: String = "O que e polimorfismo?",
        optionCount: Int = 3,
        correctIndex: Int = 0,
        explanation: String = "Polimorfismo permite tratar objetos de tipos diferentes de forma uniforme.",
        difficulty: Difficulty = Difficulty.EASY,
        category: Category = Category.OOP
    ): QuestionContent {
        val options = (0 until optionCount).map {
            QuestionOption(OptionId("opt-$it"), "Alternativa $it")
        }
        return QuestionContent(
            statement = statement,
            options = options,
            correctOption = options[correctIndex].id,
            explanation = explanation,
            category = category,
            difficulty = difficulty
        )
    }

    fun question(
        id: String = "q-1",
        difficulty: Difficulty = Difficulty.EASY,
        category: Category = Category.OOP
    ): Question = Question.create(
        id = QuestionId(id),
        content = questionContent(difficulty = difficulty, category = category),
        now = NOW
    )

    fun user(
        id: String = "u-1",
        username: String = "user-1",
        role: Role = Role.USER
    ): User = User.create(
        id = UserId(id),
        profile = UserProfile(
            username = Username.of(username),
            email = Email.of("$username@codelong.dev"),
            passwordHash = PasswordHash("hashed:secret123"),
            role = role
        ),
        now = NOW
    )

    fun game(
        id: String = "g-1",
        userId: String = "u-1",
        username: String = "user-1",
        difficulties: List<Difficulty> = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)
    ): Game {
        val questions = difficulties.mapIndexed { index, difficulty ->
            question(id = "q-$index", difficulty = difficulty).snapshot()
        }
        return Game.newGame(
            id = GameId(id),
            setup = GameSetup(UserId(userId), username, questions, GameMode.CLASSIC),
            startedAt = NOW
        )
    }
}