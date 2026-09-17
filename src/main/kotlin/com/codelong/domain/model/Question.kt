package com.codelong.domain.model

import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameQuestion
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.QuestionStatus
import java.time.Instant

class Question private constructor(
    val id: QuestionId,
    val statement: String,
    private val options: List<QuestionOption>,
    private val correctOption: OptionId,
    private val explanation: String,
    private val category: Category,
    private val difficulty: Difficulty,
    private val status: QuestionStatus,
    val createdAt: Instant,
    private var updatedAt: Instant
) {

    fun options(): List<QuestionOption> = options

    fun correctOption(): OptionId = correctOption

    fun explanation(): String = explanation

    fun category(): Category = category

    fun difficulty(): Difficulty = difficulty

    fun status(): QuestionStatus = status

    fun updatedAt(): Instant = updatedAt

    fun isActive(): Boolean = status == QuestionStatus.ACTIVE

    fun hasOption(optionId: OptionId): Boolean = options.any { it.id == optionId }

    fun isCorrectOption(optionId: OptionId): Boolean = correctOption == optionId

    fun activate(): Question = withStatus(QuestionStatus.ACTIVE)

    fun deactivate(): Question = withStatus(QuestionStatus.INACTIVE)

    private fun withStatus(newStatus: QuestionStatus): Question {
        if (newStatus == status) return this
        return Question(
            id = id,
            statement = statement,
            options = options,
            correctOption = correctOption,
            explanation = explanation,
            category = category,
            difficulty = difficulty,
            status = newStatus,
            createdAt = createdAt,
            updatedAt = Instant.now()
        )
    }

    fun update(
        statement: String,
        options: List<QuestionOption>,
        correctOption: OptionId,
        explanation: String,
        category: Category,
        difficulty: Difficulty,
        now: Instant
    ): Question = Question(
        id = id,
        statement = statement,
        options = options,
        correctOption = correctOption,
        explanation = explanation,
        category = category,
        difficulty = difficulty,
        status = status,
        createdAt = createdAt,
        updatedAt = now
    )

    fun snapshot(): GameQuestion = GameQuestion(
        id = id,
        statement = statement,
        options = options,
        correctOption = correctOption,
        explanation = explanation,
        category = category,
        difficulty = difficulty
    )

    /** Reconstructor usado por ports de persistencia e testes. */
    fun copyWith(
        statement: String = this.statement,
        options: List<QuestionOption> = this.options,
        correctOption: OptionId = this.correctOption,
        explanation: String = this.explanation,
        category: Category = this.category,
        difficulty: Difficulty = this.difficulty,
        status: QuestionStatus = this.status,
        updatedAt: Instant = this.updatedAt
    ): Question = Question(
        id = id,
        statement = statement,
        options = options,
        correctOption = correctOption,
        explanation = explanation,
        category = category,
        difficulty = difficulty,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun create(
            id: QuestionId,
            statement: String,
            options: List<QuestionOption>,
            correctOption: OptionId,
            explanation: String,
            category: Category,
            difficulty: Difficulty,
            now: Instant
        ): Question {
            requireValid(statement, options, correctOption, explanation)
            return Question(
                id = id,
                statement = statement,
                options = options,
                correctOption = correctOption,
                explanation = explanation,
                category = category,
                difficulty = difficulty,
                status = QuestionStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        }

        private fun requireValid(
            statement: String,
            options: List<QuestionOption>,
            correctOption: OptionId,
            explanation: String
        ) {
            if (statement.isBlank() || statement.length > 500) {
                throw InvalidInputException("question.statement.invalid", "Statement must not be blank and at most 500 chars")
            }
            if (options.size < 2) {
                throw InvalidInputException("question.options.invalid", "A question must have at least 2 options")
            }
            if (options.any { it.text.isBlank() }) {
                throw InvalidInputException("question.options.invalid", "Option text must not be blank")
            }
            if (options.map { it.id }.distinct().size != options.size) {
                throw InvalidInputException("question.options.invalid", "Option ids must be unique")
            }
            if (options.none { it.id == correctOption }) {
                throw InvalidInputException("question.correctOption.invalid", "The correct option must be one of the options")
            }
            if (explanation.isBlank() || explanation.length > 1000) {
                throw InvalidInputException("question.explanation.invalid", "Explanation must not be blank and at most 1000 chars")
            }
        }
    }
}