package com.codelong.application.command

import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.QuestionOption

data class QuestionOptionCommand(
    val id: String,
    val text: String
) {
    fun toDomain(): QuestionOption = QuestionOption(OptionId(id.trim()), text.trim())
}