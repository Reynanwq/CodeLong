package com.codelong.support

import com.codelong.domain.port.PasswordEncoder
import com.codelong.domain.port.TokenService
import com.codelong.domain.valueobject.PasswordHash
import com.codelong.domain.valueobject.TokenClaims
import java.time.Clock
import java.time.ZoneOffset

class FakePasswordEncoder : PasswordEncoder {

    override fun encode(rawPassword: String): PasswordHash = PasswordHash("hashed:$rawPassword")

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean =
        hash.value == "hashed:$rawPassword"
}

class FakeTokenService : TokenService {

    override fun generate(claims: TokenClaims): String = "token-${claims.userId.value}"

    override fun parse(token: String): TokenClaims = throw UnsupportedOperationException("Nao usado nos testes")
}

object TestClock {
    val fixed: Clock = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC)
}