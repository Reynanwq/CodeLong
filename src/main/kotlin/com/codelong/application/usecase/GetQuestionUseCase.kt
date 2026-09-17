package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.QuestionId

interface GetQuestionUseCase {
    fun get(questionId: QuestionId): Question
}


class GetQuestionUseCaseImpl(
    private val questionRepository: QuestionRepository
) : GetQuestionUseCase {


    override fun get(questionId: QuestionId): Question =
        questionRepository.findById(questionId)
            ?: throw Errors.questionNotFound()
}