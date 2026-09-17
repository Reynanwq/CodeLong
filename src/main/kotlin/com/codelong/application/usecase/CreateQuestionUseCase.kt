package com.codelong.application.usecase

import com.codelong.application.command.CreateQuestionCommand
import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.valueobject.Ids
import java.time.Clock

interface CreateQuestionUseCase {
    fun create(command: CreateQuestionCommand): Question
}


class CreateQuestionUseCaseImpl(
    private val questionRepository: QuestionRepository,
    private val clock: Clock
) : CreateQuestionUseCase {


    override fun create(command: CreateQuestionCommand): Question {
        val question = Question.create(
            id = Ids.newQuestionId(),
            content = command.toContent(),
            now = clock.instant()
        )
        return questionRepository.save(question)
    }
}