package com.codelong.application.usecase

import com.codelong.application.command.QuestionSearchQuery
import com.codelong.domain.port.QuestionPage
import com.codelong.domain.port.QuestionRepository

interface ListQuestionsUseCase {
    fun list(query: QuestionSearchQuery): QuestionPage
}


class ListQuestionsUseCaseImpl(
    private val questionRepository: QuestionRepository
) : ListQuestionsUseCase {


    override fun list(query: QuestionSearchQuery): QuestionPage =
        questionRepository.search(query.toSearch())
}