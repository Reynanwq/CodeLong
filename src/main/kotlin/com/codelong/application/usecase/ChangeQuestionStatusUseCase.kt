package com.codelong.application.usecase

import com.codelong.domain.exception.DomainException

import com.codelong.application.command.ChangeQuestionStatusCommand
import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import java.time.Clock

class ChangeQuestionStatusUseCase(
    private val questionRepository: QuestionRepository,
    private val clock: Clock
) {

    fun change(command: ChangeQuestionStatusCommand): Question {
        val question = questionRepository.findById(command.questionId)
            ?: throw DomainException.notFound("QUESTION_NOT_FOUND", "Question not found")

        val now = clock.instant()
        question.changeStatus(command.active, now)

        return questionRepository.save(question)
    }
}