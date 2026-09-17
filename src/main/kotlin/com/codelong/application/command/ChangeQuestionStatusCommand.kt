package com.codelong.application.command

import com.codelong.domain.valueobject.QuestionId

data class ChangeQuestionStatusCommand(
    val questionId: QuestionId,
    val active: Boolean
)