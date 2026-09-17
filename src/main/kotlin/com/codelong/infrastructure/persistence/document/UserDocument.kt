package com.codelong.infrastructure.persistence.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = MongoSchema.Collection.USERS)
data class UserDocument(
    @field:Id val id: String = "",
    @field:Indexed(unique = true) val username: String = "",
    @field:Indexed(unique = true) val email: String = "",
    val passwordHash: String = "",
    val role: String = "",
    val status: String = "",
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH
)
