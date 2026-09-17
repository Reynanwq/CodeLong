package com.codelong.application.usecase

import com.codelong.application.command.UserSearchQuery
import com.codelong.domain.port.UserPage
import com.codelong.domain.port.UserRepository

interface ListUsersUseCase {
    fun list(query: UserSearchQuery): UserPage
}


class ListUsersUseCaseImpl(
    private val userRepository: UserRepository
) : ListUsersUseCase {


    override fun list(query: UserSearchQuery): UserPage =
        userRepository.search(query.toSearch())
}
