package com.codelong.application.command

import com.codelong.domain.valueobject.UserId

data class ChangeUserStatusCommand(
    val userId: UserId,
    val active: Boolean
)
