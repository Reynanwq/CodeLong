package com.codelong.infrastructure.persistence.repository

import com.codelong.infrastructure.persistence.document.GameDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringGameDataRepository : MongoRepository<GameDocument, String> {
    fun findByUserId(userId: String): List<GameDocument>
}