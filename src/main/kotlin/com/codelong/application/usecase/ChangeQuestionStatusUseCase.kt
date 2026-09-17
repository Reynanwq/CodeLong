package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.ChangeQuestionStatusCommand
import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import java.time.Clock

interface ChangeQuestionStatusUseCase {
    fun change(command: ChangeQuestionStatusCommand): Question
}


class ChangeQuestionStatusUseCaseImpl(
    private val questionRepository: QuestionRepository,
    private val clock: Clock
) : ChangeQuestionStatusUseCase {


    override fun change(command: ChangeQuestionStatusCommand): Question {
        val question = questionRepository.findById(command.questionId)
            ?: throw Errors.questionNotFound()

        val now = clock.instant()
        question.changeStatus(command.active, now)

        return questionRepository.save(question)
    }
}