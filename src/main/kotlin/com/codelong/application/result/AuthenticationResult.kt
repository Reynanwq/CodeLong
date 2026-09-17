package com.codelong.application.result

import com.codelong.domain.model.User

data class AuthenticationResult(
    val user: User,
    val token: String
)