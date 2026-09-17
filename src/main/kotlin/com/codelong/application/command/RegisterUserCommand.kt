package com.codelong.application.command

data class RegisterUserCommand(
    val username: String,
    val email: String,
    val password: String
)