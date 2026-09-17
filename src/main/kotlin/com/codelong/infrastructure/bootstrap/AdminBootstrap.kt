package com.codelong.infrastructure.bootstrap

import com.codelong.application.service.UserFactory
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.security.SecurityProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

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

    private val logger = LoggerFactory.getLogger(AdminBootstrap::class.java)

    override fun run(args: ApplicationArguments) {
        val username = properties.admin.username.trim()
        val password = properties.admin.password
        if (username.isEmpty() || password.isEmpty()) {
            return
        }

        val value = Username.of(username)
        if (userRepository.existsByUsername(value)) {
            return
        }

        userRepository.save(userFactory.createAdmin(value, Email.of("$username@codelong.local"), password))
        logger.info("Admin bootstrap: usuario '{}' criado", username)
    }
}