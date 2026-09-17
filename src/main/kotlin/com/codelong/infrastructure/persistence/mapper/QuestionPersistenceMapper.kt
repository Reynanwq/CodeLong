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
            id = state.idText(),
            statement = state.statementText(),
            options = state.options().map { OptionDocument(it.idText(), it.text) },
            correctOption = state.correctOptionText(),
            explanation = state.explanationText(),
            category = state.categoryName(),
            difficulty = state.difficultyName(),
            status = state.statusName(),
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