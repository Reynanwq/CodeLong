package com.codelong.domain.model

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username
import java.time.Instant

class User private constructor(
    val id: UserId,
    val username: Username,
    val email: Email,
    private var passwordHash: PasswordHash,
    val role: Role,
    val status: AccountStatus,
    val createdAt: Instant,
    private var updatedAt: Instant
) {

    fun passwordHash(): PasswordHash = passwordHash

    fun updatedAt(): Instant = updatedAt

    fun changePassword(newHash: PasswordHash, now: Instant): User = apply {
        passwordHash = newHash
        updatedAt = now
    }

    fun isActive(): Boolean = status == AccountStatus.ACTIVE

    fun isAdmin(): Boolean = role == Role.ADMIN

    fun matchesIdentity(identifier: String): Boolean =
        username.value.equals(identifier, ignoreCase = true) ||
            email.value.equals(identifier, ignoreCase = true)

    companion object {
        fun create(
            id: UserId,
            username: Username,
            email: Email,
            passwordHash: PasswordHash,
            role: Role,
            now: Instant
        ): User = User(
            id = id,
            username = username,
            email = email,
            passwordHash = passwordHash,
            role = role,
            status = AccountStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
    }
}