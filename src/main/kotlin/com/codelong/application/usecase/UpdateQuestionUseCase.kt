package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.UpdateQuestionCommand
import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import java.time.Clock

interface UpdateQuestionUseCase {
    fun update(command: UpdateQuestionCommand): Question
}


class UpdateQuestionUseCaseImpl(
    private val questionRepository: QuestionRepository,
    private val clock: Clock
) : UpdateQuestionUseCase {


    override fun update(command: UpdateQuestionCommand): Question {
        val question = questionRepository.findById(command.questionId)
            ?: throw Errors.questionNotFound()

        question.update(command.toContent(), clock.instant())
        return questionRepository.save(question)
    }
}