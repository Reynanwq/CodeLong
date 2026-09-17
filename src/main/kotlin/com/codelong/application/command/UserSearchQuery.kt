package com.codelong.application.command

import com.codelong.domain.port.UserSearch
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role

data class UserSearchQuery(
    val status: AccountStatus? = null,
    val role: Role? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    fun toSearch(): UserSearch = UserSearch(
        status = status,
        role = role,
        page = page,
        size = size
    )
}
