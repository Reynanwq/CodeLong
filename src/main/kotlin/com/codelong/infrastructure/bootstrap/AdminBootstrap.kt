package com.codelong.infrastructure.bootstrap

import com.codelong.application.service.UserFactory
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.security.SecurityProperties
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

private const val ADMIN_EMAIL_SUFFIX = "@codelong.local"

/**
 * Garante um usuario ADMIN inicial quando codelong.admin.username/password
 * estao configurados. Nao faz nada se ja existir ou se a config estiver vazia.
 */
@Component
class AdminBootstrap(
    private val userRepository: UserRepository,
    private val userFactory: UserFactory,
    private val properties: SecurityProperties
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val username = properties.admin.username.trim()
        val password = properties.admin.password

        (username.isNotEmpty() && password.isNotEmpty()).takeIf { it }?.let {
            bootstrap(username, password)
        }
    }

    private fun bootstrap(username: String, password: String) {
        Username.of(username)
            .takeUnless { userRepository.existsByUsername(it) }
            ?.let { value ->
                userRepository.save(
                    userFactory.createAdmin(value, Email.of(username + ADMIN_EMAIL_SUFFIX), password)
                )
            }
    }
}