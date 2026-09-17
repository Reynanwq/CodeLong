package com.codelong.application.command

data class LoginCommand(
    val identifier: String,
    val password: String
)