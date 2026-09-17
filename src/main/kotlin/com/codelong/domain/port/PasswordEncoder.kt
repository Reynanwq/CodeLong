package com.codelong.domain.port

import com.codelong.domain.valueobject.PasswordHash

interface PasswordEncoder {
    fun encode(rawPassword: String): PasswordHash
    fun matches(rawPassword: String, hash: PasswordHash): Boolean
}