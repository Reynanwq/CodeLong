package com.codelong.domain.model

import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.UserProfile
import com.codelong.domain.valueobject.UserState
import com.codelong.domain.valueobject.Username
import java.time.Instant

class User private constructor(
    val id: UserId,
    private var profile: UserProfile,
    private var status: AccountStatus,
    val createdAt: Instant,
    private var updatedAt: Instant
) {

    val username: Username get() = profile.username

    val email: Email get() = profile.email

    val role: Role get() = profile.role

    fun passwordHash(): PasswordHash = profile.passwordHash

    fun status(): AccountStatus = status

    fun updatedAt(): Instant = updatedAt

    fun changePassword(newHash: PasswordHash, now: Instant): User = apply {
        profile = profile.withPasswordHash(newHash)
        updatedAt = now
    }

    fun deactivate(now: Instant): User = apply {
        status = AccountStatus.INACTIVE
        updatedAt = now
    }

    fun isActive(): Boolean = status == AccountStatus.ACTIVE

    fun isAdmin(): Boolean = role == Role.ADMIN

    fun matchesIdentity(identifier: String): Boolean =
        username.value.equals(identifier, ignoreCase = true) ||
            email.value.equals(identifier, ignoreCase = true)

    fun state(): UserState = UserState(
        id = id,
        profile = profile,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun create(id: UserId, profile: UserProfile, now: Instant): User =
            User(
                id = id,
                profile = profile,
                status = AccountStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

        fun reconstitute(state: UserState): User =
            User(
                id = state.id,
                profile = state.profile,
                status = state.status,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt
            )
    }
}