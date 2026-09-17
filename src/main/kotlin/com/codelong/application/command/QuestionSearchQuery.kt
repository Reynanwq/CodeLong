package com.codelong.application.command

import com.codelong.domain.port.QuestionSearch
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionStatus

data class QuestionSearchQuery(
    val status: QuestionStatus? = null,
    val category: Category? = null,
    val difficulty: Difficulty? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    fun toSearch(): QuestionSearch = QuestionSearch(
        status = status,
        category = category,
        difficulty = difficulty,
        page = page,
        size = size
    )
}