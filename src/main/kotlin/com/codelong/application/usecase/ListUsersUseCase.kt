package com.codelong.application.usecase

import com.codelong.application.command.UserSearchQuery
import com.codelong.domain.port.UserPage
import com.codelong.domain.port.UserRepository

class ListUsersUseCase(
    private val userRepository: UserRepository
) {

    fun list(query: UserSearchQuery): UserPage =
        userRepository.search(query.toSearch())
}
