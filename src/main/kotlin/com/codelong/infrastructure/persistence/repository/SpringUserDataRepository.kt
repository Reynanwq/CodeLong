package com.codelong.infrastructure.persistence.repository

import com.codelong.infrastructure.persistence.document.UserDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringUserDataRepository : MongoRepository<UserDocument, String> {
    fun findByUsername(username: String): UserDocument?
    fun findByEmail(email: String): UserDocument?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}