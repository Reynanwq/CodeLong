package com.codelong.domain.model

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.GameQuestion
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionContent
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.QuestionState
import com.codelong.domain.valueobject.QuestionStatus
import java.time.Instant

class Question private constructor(
    val id: QuestionId,
    private var content: QuestionContent,
    status: QuestionStatus,
    val createdAt: Instant,
    updatedAt: Instant
) {

    var status: QuestionStatus = status
        private set

    var updatedAt: Instant = updatedAt
        private set

    val statement: String get() = content.statementText

    val options: List<QuestionOption> get() = content.options

    val correctOption: OptionId get() = content.correctOption

    val explanation: String get() = content.explanationText

    val category: Category get() = content.category

    val difficulty: Difficulty get() = content.difficulty

    val isActive: Boolean get() = status == QuestionStatus.ACTIVE

    val idText: String get() = id.value

    val correctOptionText: String get() = content.correctOptionText

    val categoryName: String get() = content.categoryName

    val difficultyName: String get() = content.difficultyName

    val statusName: String get() = status.name

    fun hasOption(optionId: OptionId): Boolean = content.hasOption(optionId)

    fun isCorrectOption(optionId: OptionId): Boolean = content.isCorrect(optionId)

    fun activate(now: Instant): Question = apply {
        status = QuestionStatus.ACTIVE
        updatedAt = now
    }

    fun deactivate(now: Instant): Question = apply {
        status = QuestionStatus.INACTIVE
        updatedAt = now
    }

    /** Aplica o status correspondente a [active] em uma unica operacao. */
    fun changeStatus(active: Boolean, now: Instant): Question = apply {
        status = QuestionStatus.of(active)
        updatedAt = now
    }

    fun update(newContent: QuestionContent, now: Instant): Question = apply {
        content = newContent
        updatedAt = now
    }

    fun snapshot(): GameQuestion = GameQuestion(
        id = id,
        statement = content.statement,
        options = content.options,
        correctOption = content.correctOption,
        explanation = content.explanation,
        category = content.category,
        difficulty = content.difficulty
    )

    fun state(): QuestionState = QuestionState(
        id = id,
        content = content,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun create(id: QuestionId, content: QuestionContent, now: Instant): Question =
            Question(
                id = id,
                content = content,
                status = QuestionStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

        fun reconstitute(state: QuestionState): Question =
            Question(
                id = state.id,
                content = state.content,
                status = state.status,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt
            )
    }
}
