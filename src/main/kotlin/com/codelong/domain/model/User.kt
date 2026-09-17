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
    status: AccountStatus,
    val createdAt: Instant,
    updatedAt: Instant
) {

    var status: AccountStatus = status
        private set

    var updatedAt: Instant = updatedAt
        private set

    val username: Username get() = profile.username

    val email: Email get() = profile.email

    val role: Role get() = profile.role

    val passwordHash: PasswordHash get() = profile.passwordHash

    val idText: String get() = id.value

    val usernameText: String get() = profile.usernameText

    val emailText: String get() = profile.emailText

    val roleName: String get() = profile.roleName

    val statusName: String get() = status.name

    val isActive: Boolean get() = status == AccountStatus.ACTIVE

    val isAdmin: Boolean get() = role == Role.ADMIN

    fun changePassword(newHash: PasswordHash, now: Instant): User = apply {
        profile = profile.withPasswordHash(newHash)
        updatedAt = now
    }

    fun deactivate(now: Instant): User = apply {
        status = AccountStatus.INACTIVE
        updatedAt = now
    }

    fun activate(now: Instant): User = apply {
        status = AccountStatus.ACTIVE
        updatedAt = now
    }

    /** Aplica o status correspondente a [active] em uma unica operacao. */
    fun changeStatus(active: Boolean, now: Instant): User = apply {
        status = AccountStatus.of(active)
        updatedAt = now
    }

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
