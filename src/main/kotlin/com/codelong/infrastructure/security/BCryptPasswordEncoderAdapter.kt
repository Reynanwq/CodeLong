package com.codelong.infrastructure.security

import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.valueobject.PasswordHash
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordEncoderAdapter : PasswordEncoder {

    private val delegate = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): PasswordHash =
        PasswordHash(delegate.encode(rawPassword) ?: error("Could not encode the password"))

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean =
        delegate.matches(rawPassword, hash.value)
}