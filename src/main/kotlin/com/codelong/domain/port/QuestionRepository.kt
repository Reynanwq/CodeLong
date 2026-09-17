package com.codelong.domain.port

import com.codelong.domain.model.Question
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus

data class QuestionSearch(
    val status: QuestionStatus? = null,
    val category: Category? = null,
    val difficulty: Difficulty? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    init {
        Pagination.requireValid(page, size)
    }
}

data class QuestionPage(
    val items: List<Question>,
    val totalElements: Long,
    val page: Int,
    val size: Int
)

interface QuestionRepository {
    fun save(question: Question): Question
    fun findById(id: QuestionId): Question?
    fun findAllActive(): List<Question>
    fun countActive(): Long
    fun search(search: QuestionSearch): QuestionPage
    fun deleteById(id: QuestionId)
}