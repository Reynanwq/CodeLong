package com.codelong.infrastructure.web.controller

import com.codelong.application.command.ChangeUserStatusCommand
import com.codelong.application.command.UserSearchQuery
import com.codelong.application.usecase.ChangeUserStatusUseCase
import com.codelong.application.usecase.ListUsersUseCase
import com.codelong.domain.valueobject.AccountStatus
import com.codelong.domain.valueobject.Role
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.AdminUserResponse
import com.codelong.infrastructure.web.dto.ChangeUserStatusRequest
import com.codelong.infrastructure.web.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/admin/users"
private const val DEFAULT_PAGE = "0"
private const val DEFAULT_SIZE = "20"
private const val STATUS_PATH = "/{userId}/status"

@RestController
@RequestMapping(BASE_PATH)
class AdminUserController(
    private val listUsersUseCase: ListUsersUseCase,
    private val changeUserStatusUseCase: ChangeUserStatusUseCase
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(defaultValue = DEFAULT_PAGE) page: Int,
        @RequestParam(defaultValue = DEFAULT_SIZE) size: Int
    ): PageResponse<AdminUserResponse> {
        val query = UserSearchQuery(
            status = status?.let { AccountStatus.fromName(it) },
            role = role?.let { Role.fromName(it) },
            page = page,
            size = size
        )
        val result = listUsersUseCase.list(query)
        return PageResponse.of(
            items = result.items.map(AdminUserResponse::from),
            totalElements = result.totalElements,
            page = result.page,
            size = result.size
        )
    }

    @PatchMapping(STATUS_PATH)
    fun changeStatus(
        @PathVariable userId: String,
        @Valid @RequestBody request: ChangeUserStatusRequest,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): AdminUserResponse =
        AdminUserResponse.from(
            changeUserStatusUseCase.change(
                ChangeUserStatusCommand(UserId(userId), request.active),
                principal.userId
            )
        )
}
