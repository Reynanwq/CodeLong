package com.codelong.application.command

data class ChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String
)
