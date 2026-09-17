package com.codelong.application.command

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId

data class UpdateQuestionCommand(
    val questionId: QuestionId,
    override val statement: String,
    override val options: List<QuestionOptionCommand>,
    override val correctOption: String,
    override val explanation: String,
    override val category: Category,
    override val difficulty: Difficulty
) : QuestionContentCommand