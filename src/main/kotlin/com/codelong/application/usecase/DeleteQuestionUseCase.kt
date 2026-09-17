package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.QuestionId

class DeleteQuestionUseCase(
    private val questionRepository: QuestionRepository
) {

    fun delete(questionId: QuestionId) {
        questionRepository.findById(questionId)
            ?: throw Errors.questionNotFound()
        questionRepository.deleteById(questionId)
    }
}