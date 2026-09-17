package com.codelong.infrastructure.persistence.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = MongoSchema.Collection.QUESTIONS)
@CompoundIndex(name = MongoSchema.Index.QUESTION_SEARCH, def = MongoSchema.Index.QUESTION_SEARCH_DEF)
data class QuestionDocument(
    @field:Id val id: String = "",
    val statement: String = "",
    val options: List<OptionDocument> = emptyList(),
    val correctOption: String = "",
    val explanation: String = "",
    val category: String = "",
    val difficulty: String = "",
    val status: String = "",
    @field:Indexed val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH
)
