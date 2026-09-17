package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionContent
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionOption
import com.codelong.domain.valueobject.QuestionState
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.infrastructure.persistence.document.OptionDocument
import com.codelong.infrastructure.persistence.document.QuestionDocument

object QuestionPersistenceMapper {

    fun toDocument(question: Question): QuestionDocument {
        val state = question.state()
        return QuestionDocument(
            id = state.id.value,
            statement = state.content.statement,
            options = state.content.options.map { OptionDocument(it.id.value, it.text) },
            correctOption = state.content.correctOption.value,
            explanation = state.content.explanation,
            category = state.content.category.name,
            difficulty = state.content.difficulty.name,
            status = state.status.name,
            createdAt = state.createdAt,
            updatedAt = state.updatedAt
        )
    }

    fun toDomain(document: QuestionDocument): Question = Question.reconstitute(
        QuestionState(
            id = QuestionId(document.id),
            content = QuestionContent(
                statement = document.statement,
                options = document.options.map { QuestionOption(OptionId(it.id), it.text) },
                correctOption = OptionId(document.correctOption),
                explanation = document.explanation,
                category = Category.fromName(document.category),
                difficulty = Difficulty.valueOf(document.difficulty)
            ),
            status = QuestionStatus.valueOf(document.status),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
        )
    )
}