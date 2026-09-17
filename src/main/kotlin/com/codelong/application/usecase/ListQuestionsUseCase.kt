package com.codelong.application.usecase

import com.codelong.application.command.QuestionSearchQuery
import com.codelong.domain.port.QuestionPage
import com.codelong.domain.port.QuestionRepository

class ListQuestionsUseCase(
    private val questionRepository: QuestionRepository
) {

    fun list(query: QuestionSearchQuery): QuestionPage =
        questionRepository.search(query.toSearch())
}