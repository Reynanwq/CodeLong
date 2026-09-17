package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.QuestionId

interface DeleteQuestionUseCase {
    fun delete(questionId: QuestionId)
}


class DeleteQuestionUseCaseImpl(
    private val questionRepository: QuestionRepository
) : DeleteQuestionUseCase {


    override fun delete(questionId: QuestionId) {
        questionRepository.findById(questionId)
            ?: throw Errors.questionNotFound()
        questionRepository.deleteById(questionId)
    }
}