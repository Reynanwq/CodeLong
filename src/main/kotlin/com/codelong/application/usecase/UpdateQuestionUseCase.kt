package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.UpdateQuestionCommand
import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import java.time.Clock

class UpdateQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val clock: Clock
) {

    fun update(command: UpdateQuestionCommand): Question {
        val question = questionRepository.findById(command.questionId)
            ?: throw Errors.questionNotFound()

        question.update(command.toContent(), clock.instant())
        return questionRepository.save(question)
    }
}