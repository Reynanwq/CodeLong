package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.QuestionId

class GetQuestionUseCase(
    private val questionRepository: QuestionRepository
) {

    fun get(questionId: QuestionId): Question =
        questionRepository.findById(questionId)
            ?: throw DomainException.notFound("QUESTION_NOT_FOUND", "Question not found")
}