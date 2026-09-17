package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.model.Game
import com.codelong.domain.valueobject.AnswerRecord
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameQuestion
import com.codelong.domain.valueobject.GameState
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.AnswerDocument
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.GameQuestionDocument
import com.codelong.infrastructure.persistence.document.OptionDocument

object GamePersistenceMapper {

    fun toDocument(game: Game): GameDocument {
        val state = game.state()
        return GameDocument(
            id = state.idText(),
            userId = state.userIdText(),
            username = state.username,
            status = state.statusName(),
            startedAt = state.startedAt,
            completedAt = state.completedAt,
            currentQuestionIndex = state.currentQuestionIndex,
            questions = state.questions.map { toQuestionDocument(it) },
            answers = state.answers.map { toAnswerDocument(it) },
            score = state.score,
            correctAnswers = state.correctAnswers,
            wrongAnswers = state.wrongAnswers,
            version = state.version
        )
    }

    fun toDomain(document: GameDocument): Game = Game.reconstitute(
        GameState(
            id = GameId(document.id),
            userId = UserId(document.userId),
            username = document.username,
            status = GameStatus.valueOf(document.status),
            startedAt = document.startedAt,
            completedAt = document.completedAt,
            currentQuestionIndex = document.currentQuestionIndex,
            questions = document.questions.map { toQuestion(it) },
            answers = document.answers.map { toAnswer(it) },
            score = document.score,
            correctAnswers = document.correctAnswers,
            wrongAnswers = document.wrongAnswers,
            version = document.version
        )
    )

    private fun toQuestionDocument(question: GameQuestion): GameQuestionDocument =
        GameQuestionDocument(
            id = question.idText(),
            statement = question.statement,
            options = question.options.map { OptionDocument(it.idText(), it.text) },
            correctOption = question.correctOptionText(),
            explanation = question.explanation,
            category = question.categoryName(),
            difficulty = question.difficultyName()
        )

    private fun toQuestion(document: GameQuestionDocument): GameQuestion = GameQuestion(
        id = QuestionId(document.id),
        statement = document.statement,
        options = document.options.map { QuestionOption(OptionId(it.id), it.text) },
        correctOption = OptionId(document.correctOption),
        explanation = document.explanation,
        category = Category.fromName(document.category),
        difficulty = Difficulty.valueOf(document.difficulty)
    )

    private fun toAnswerDocument(answer: AnswerRecord): AnswerDocument = AnswerDocument(
        questionIndex = answer.questionIndex,
        questionId = answer.questionIdText(),
        chosenOption = answer.chosenOptionText(),
        correct = answer.correct,
        earnedPoints = answer.earnedPoints,
        answeredAt = answer.answeredAt
    )

    private fun toAnswer(document: AnswerDocument): AnswerRecord = AnswerRecord(
        questionIndex = document.questionIndex,
        questionId = QuestionId(document.questionId),
        chosenOption = OptionId(document.chosenOption),
        correct = document.correct,
        earnedPoints = document.earnedPoints,
        answeredAt = document.answeredAt
    )
}