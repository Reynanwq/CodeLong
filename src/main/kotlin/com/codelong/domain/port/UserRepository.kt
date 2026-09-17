package com.codelong.domain.port

import com.codelong.domain.model.User
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username

data class UserSearch(
    val status: AccountStatus? = null,
    val role: Role? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    init {
        Pagination.requireValid(page, size)
    }
}

data class UserPage(
    val items: List<User>,
    val totalElements: Long,
    val page: Int,
    val size: Int
)

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UserId): User?
    fun findByUsername(username: Username): User?
    fun findByEmail(email: Email): User?
    fun existsByUsername(username: Username): Boolean
    fun existsByEmail(email: Email): Boolean
    fun search(search: UserSearch): UserPage
}
