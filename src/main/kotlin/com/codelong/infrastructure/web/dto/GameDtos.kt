package com.codelong.infrastructure.web.dto

import com.codelong.domain.GameRules
import com.codelong.domain.model.Game
import com.codelong.domain.valueobject.AnswerResult
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.QuestionPublic
import jakarta.validation.constraints.NotBlank
import java.time.Instant

private const val OPTION_ID_REQUIRED = "optionId is required"

data class OptionResponse(
    val id: String,
    val text: String
) {
    companion object {
        fun from(option: QuestionOption) = OptionResponse(option.idText, option.text)
    }
}

data class PublicQuestionResponse(
    val id: String,
    val statement: String,
    val options: List<OptionResponse>,
    val category: String,
    val difficulty: String,
    val deadline: Instant?,
    val timeLimitSeconds: Long
) {
    companion object {
        fun from(question: QuestionPublic, deadline: Instant? = null) = PublicQuestionResponse(
            id = question.idText,
            statement = question.statement,
            options = question.options.map(OptionResponse::from),
            category = question.categoryName,
            difficulty = question.difficultyName,
            deadline = deadline,
            timeLimitSeconds = GameRules.ANSWER_TIME_LIMIT_SECONDS
        )
    }
}

data class GameResponse(
    val id: String,
    val status: String,
    val mode: String,
    val currentQuestionIndex: Int,
    val currentQuestionDeadline: Instant,
    val totalQuestions: Int,
    val remainingQuestions: Int,
    val score: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val startedAt: Instant,
    val completedAt: Instant?
) {
    companion object {
        fun from(game: Game) = GameResponse(
            id = game.idText,
            status = game.statusName,
            mode = game.mode.name,
            currentQuestionIndex = game.currentQuestionIndex,
            currentQuestionDeadline = game.currentQuestionDeadline,
            totalQuestions = game.totalQuestions,
            remainingQuestions = game.remainingQuestions,
            score = game.score,
            correctAnswers = game.correctAnswers,
            wrongAnswers = game.wrongAnswers,
            startedAt = game.startedAt,
            completedAt = game.completedAt
        )
    }
}

data class AnswerRequest(
    @field:NotBlank(message = OPTION_ID_REQUIRED)
    val optionId: String
)

data class AnswerResponse(
    val correct: Boolean,
    val timedOut: Boolean,
    val chosenOption: String?,
    val correctOption: String,
    val explanation: String,
    val earnedPoints: Int,
    val currentScore: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val gameCompleted: Boolean,
    val questionIndex: Int,
    val totalQuestions: Int,
    val nextQuestion: PublicQuestionResponse?
) {
    companion object {
        fun from(result: AnswerResult) = AnswerResponse(
            correct = result.record.correct,
            timedOut = result.record.timedOut,
            chosenOption = result.record.chosenOptionText,
            correctOption = result.question.correctOptionText,
            explanation = result.question.explanation,
            earnedPoints = result.record.earnedPoints,
            currentScore = result.currentScore,
            correctAnswers = result.correctAnswers,
            wrongAnswers = result.wrongAnswers,
            gameCompleted = result.gameCompleted,
            questionIndex = result.questionIndex,
            totalQuestions = result.totalQuestions,
            nextQuestion = result.nextQuestion
                ?.let { PublicQuestionResponse.from(it, result.nextQuestionDeadline) }
        )
    }
}
