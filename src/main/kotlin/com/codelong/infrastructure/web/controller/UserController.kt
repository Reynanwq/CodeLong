package com.codelong.infrastructure.web.controller

import com.codelong.application.command.ChangePasswordCommand
import com.codelong.application.usecase.ChangePasswordUseCase
import com.codelong.application.usecase.GetCurrentUserUseCase
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.ChangePasswordRequest
import com.codelong.infrastructure.web.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/users"
private const val ME_PATH = "/me"
private const val PASSWORD_PATH = "/me/password"

@RestController
@RequestMapping(BASE_PATH)
class UserController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase
) {

    @GetMapping(ME_PATH)
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): UserResponse =
        UserResponse.from(getCurrentUserUseCase.get(principal.userId))

    @PatchMapping(PASSWORD_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ) {
        changePasswordUseCase.change(
            ChangePasswordCommand(request.currentPassword, request.newPassword),
            principal.userId
        )
    }
}
