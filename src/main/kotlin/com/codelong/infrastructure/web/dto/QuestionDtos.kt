package com.codelong.infrastructure.web.dto

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.CreateQuestionCommand
import com.codelong.application.command.QuestionOptionCommand
import com.codelong.application.command.UpdateQuestionCommand
import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class OptionRequest(
    @field:NotBlank(message = "option id is required")
    val id: String,

    @field:NotBlank(message = "option text is required")
    val text: String
)

data class CreateQuestionRequest(
    @field:NotBlank(message = "statement is required")
    @field:Size(max = 500, message = "statement must have at most 500 characters")
    val statement: String,

    @field:NotEmpty(message = "options are required")
    @field:Size(min = 2, message = "a question must have at least 2 options")
    val options: List<@Valid OptionRequest>,

    @field:NotBlank(message = "correctOption is required")
    val correctOption: String,

    @field:NotBlank(message = "explanation is required")
    @field:Size(max = 1000, message = "explanation must have at most 1000 characters")
    val explanation: String,

    @field:NotBlank(message = "category is required")
    val category: String,

    @field:NotBlank(message = "difficulty is required")
    val difficulty: String
)

data class UpdateQuestionRequest(
    @field:NotBlank(message = "statement is required")
    @field:Size(max = 500, message = "statement must have at most 500 characters")
    val statement: String,

    @field:NotEmpty(message = "options are required")
    @field:Size(min = 2, message = "a question must have at least 2 options")
    val options: List<@Valid OptionRequest>,

    @field:NotBlank(message = "correctOption is required")
    val correctOption: String,

    @field:NotBlank(message = "explanation is required")
    @field:Size(max = 1000, message = "explanation must have at most 1000 characters")
    val explanation: String,

    @field:NotBlank(message = "category is required")
    val category: String,

    @field:NotBlank(message = "difficulty is required")
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

private fun difficultyOf(value: String): Difficulty {
    val normalized = value.trim().uppercase()
    return Difficulty.entries.firstOrNull { it.name == normalized }
        ?: throw DomainException.invalidInput("difficulty.invalid", "Unknown difficulty: $value")
}