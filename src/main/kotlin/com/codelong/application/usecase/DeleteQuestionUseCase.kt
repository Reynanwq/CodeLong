package com.codelong.application.usecase

import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.QuestionId

class DeleteQuestionUseCase(
    private val questionRepository: QuestionRepository
) {

    fun delete(questionId: QuestionId) {
        questionRepository.findById(questionId)
            ?: throw NotFoundException("QUESTION_NOT_FOUND", "Question not found")
        questionRepository.deleteById(questionId)
    }
}