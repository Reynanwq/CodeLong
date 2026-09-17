package com.codelong.domain.port

import com.codelong.domain.valueobject.TokenClaims

interface TokenService {
    fun generate(claims: TokenClaims): String
    fun parse(token: String): TokenClaims
}