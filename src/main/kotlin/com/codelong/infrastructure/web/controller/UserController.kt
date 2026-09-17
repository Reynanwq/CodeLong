package com.codelong.infrastructure.web.controller

import com.codelong.application.usecase.GetCurrentUserUseCase
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.UserResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): UserResponse =
        UserResponse.from(getCurrentUserUseCase.get(principal.userId))
}