package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.domain.model.User
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId

interface GetCurrentUserUseCase {
    fun get(userId: UserId): User
}


class GetCurrentUserUseCaseImpl(
    private val userRepository: UserRepository
) : GetCurrentUserUseCase {


    override fun get(userId: UserId): User =
        userRepository.findById(userId)
            ?: throw Errors.userNotFound()
}