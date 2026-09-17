package com.codelong.application.command

import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty

data class CreateQuestionCommand(
    override val statement: String,
    override val options: List<QuestionOptionCommand>,
    override val correctOption: String,
    override val explanation: String,
    override val category: Category,
    override val difficulty: Difficulty
) : QuestionContentCommand