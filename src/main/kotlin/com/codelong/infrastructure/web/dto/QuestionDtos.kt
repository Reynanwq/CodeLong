package com.codelong.infrastructure.web.dto

import com.codelong.application.command.CreateQuestionCommand
import com.codelong.application.command.QuestionOptionCommand
import com.codelong.application.command.UpdateQuestionCommand
import com.codelong.domain.exception.Errors
import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

private const val OPTION_ID_REQUIRED = "option id is required"
private const val OPTION_TEXT_REQUIRED = "option text is required"
private const val STATEMENT_REQUIRED = "statement is required"
private const val STATEMENT_MAX = "statement must have at most 500 characters"
private const val OPTIONS_REQUIRED = "options are required"
private const val OPTIONS_MIN = "a question must have at least 2 options"
private const val CORRECT_OPTION_REQUIRED = "correctOption is required"
private const val EXPLANATION_REQUIRED = "explanation is required"
private const val EXPLANATION_MAX = "explanation must have at most 1000 characters"
private const val CATEGORY_REQUIRED = "category is required"
private const val DIFFICULTY_REQUIRED = "difficulty is required"
private const val STATEMENT_MAX_LENGTH = 500
private const val EXPLANATION_MAX_LENGTH = 1000
private const val MIN_OPTIONS = 2

data class OptionRequest(
    @field:NotBlank(message = OPTION_ID_REQUIRED)
    val id: String,

    @field:NotBlank(message = OPTION_TEXT_REQUIRED)
    val text: String
)

data class CreateQuestionRequest(
    @field:NotBlank(message = STATEMENT_REQUIRED)
    @field:Size(max = STATEMENT_MAX_LENGTH, message = STATEMENT_MAX)
    val statement: String,

    @field:NotEmpty(message = OPTIONS_REQUIRED)
    @field:Size(min = MIN_OPTIONS, message = OPTIONS_MIN)
    val options: List<@Valid OptionRequest>,

    @field:NotBlank(message = CORRECT_OPTION_REQUIRED)
    val correctOption: String,

    @field:NotBlank(message = EXPLANATION_REQUIRED)
    @field:Size(max = EXPLANATION_MAX_LENGTH, message = EXPLANATION_MAX)
    val explanation: String,

    @field:NotBlank(message = CATEGORY_REQUIRED)
    val category: String,

    @field:NotBlank(message = DIFFICULTY_REQUIRED)
    val difficulty: String
)

data class UpdateQuestionRequest(
    @field:NotBlank(message = STATEMENT_REQUIRED)
    @field:Size(max = STATEMENT_MAX_LENGTH, message = STATEMENT_MAX)
    val statement: String,

    @field:NotEmpty(message = OPTIONS_REQUIRED)
    @field:Size(min = MIN_OPTIONS, message = OPTIONS_MIN)
    val options: List<@Valid OptionRequest>,

    @field:NotBlank(message = CORRECT_OPTION_REQUIRED)
    val correctOption: String,

    @field:NotBlank(message = EXPLANATION_REQUIRED)
    @field:Size(max = EXPLANATION_MAX_LENGTH, message = EXPLANATION_MAX)
    val explanation: String,

    @field:NotBlank(message = CATEGORY_REQUIRED)
    val category: String,

    @field:NotBlank(message = DIFFICULTY_REQUIRED)
    val difficulty: String
)

data class ChangeQuestionStatusRequest(
    val active: Boolean
)

data class QuestionResponse(
    val id: String,
    val statement: String,
    val options: List<OptionResponse>,
    val correctOption: String,
    val explanation: String,
    val category: String,
    val difficulty: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(question: Question) = QuestionResponse(
            id = question.id.value,
            statement = question.statement(),
            options = question.options().map(OptionResponse::from),
            correctOption = question.correctOption().value,
            explanation = question.explanation(),
            category = question.category().name,
            difficulty = question.difficulty().name,
            status = question.status().name,
            createdAt = question.createdAt,
            updatedAt = question.updatedAt()
        )
    }
}

fun CreateQuestionRequest.toCommand(): CreateQuestionCommand = CreateQuestionCommand(
    statement = statement,
    options = options.map { QuestionOptionCommand(it.id, it.text) },
    correctOption = correctOption,
    explanation = explanation,
    category = Category.fromName(category),
    difficulty = difficultyOf(difficulty)
)

fun UpdateQuestionRequest.toCommand(questionId: QuestionId): UpdateQuestionCommand = UpdateQuestionCommand(
    questionId = questionId,
    statement = statement,
    options = options.map { QuestionOptionCommand(it.id, it.text) },
    correctOption = correctOption,
    explanation = explanation,
    category = Category.fromName(category),
    difficulty = difficultyOf(difficulty)
)

private fun difficultyOf(value: String): Difficulty =
    Difficulty.entries.firstOrNull { it.name == value.trim().uppercase() }
        ?: throw Errors.unknownDifficulty(value)
