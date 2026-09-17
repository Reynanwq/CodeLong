package com.codelong.infrastructure.persistence.mapper

import com.codelong.domain.model.User
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.UserProfile
import com.codelong.domain.valueobject.UserState
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.persistence.document.UserDocument

object UserPersistenceMapper {

    fun toDocument(user: User): UserDocument {
        val state = user.state()
        return UserDocument(
            id = state.idText,
            username = state.usernameText,
            email = state.emailText,
            passwordHash = state.passwordHashText,
            role = state.roleName,
            status = state.statusName,
            createdAt = state.createdAt,
            updatedAt = state.updatedAt
        )
    }

    fun toDomain(document: UserDocument): User = User.reconstitute(
        UserState(
            id = UserId(document.id),
            profile = UserProfile(
                username = Username.of(document.username),
                email = Email.of(document.email),
                passwordHash = PasswordHash(document.passwordHash),
                role = Role.fromName(document.role)
            ),
            status = AccountStatus.valueOf(document.status),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
        )
    )
}