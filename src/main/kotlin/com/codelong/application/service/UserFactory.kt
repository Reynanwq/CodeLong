package com.codelong.application.service

import com.codelong.domain.model.User
import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Ids
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserProfile
import com.codelong.domain.valueobject.Username
import java.time.Clock

/**
 * Cria usuarios aplicando hashing de senha e carimbos de tempo de forma
 * consistente em toda a aplicacao.
 */
class UserFactory(
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) {

    fun createUser(username: Username, email: Email, rawPassword: String): User =
        create(username, email, rawPassword, Role.USER)

    fun createAdmin(username: Username, email: Email, rawPassword: String): User =
        create(username, email, rawPassword, Role.ADMIN)

    private fun create(username: Username, email: Email, rawPassword: String, role: Role): User {
        val profile = UserProfile(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(rawPassword),
            role = role
        )
        return User.create(Ids.newUserId(), profile, clock.instant())
    }
}