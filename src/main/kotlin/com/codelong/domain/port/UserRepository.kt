package com.codelong.domain.port

import com.codelong.domain.model.User
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UserId): User?
    fun findByUsername(username: Username): User?
    fun findByEmail(email: Email): User?
    fun existsByUsername(username: Username): Boolean
    fun existsByEmail(email: Email): Boolean
}