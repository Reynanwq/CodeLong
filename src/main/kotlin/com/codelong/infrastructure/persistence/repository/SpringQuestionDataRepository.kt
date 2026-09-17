package com.codelong.infrastructure.persistence.repository

import com.codelong.infrastructure.persistence.document.QuestionDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringQuestionDataRepository : MongoRepository<QuestionDocument, String> {
    fun findByStatus(status: String): List<QuestionDocument>
    fun countByStatus(status: String): Long
}