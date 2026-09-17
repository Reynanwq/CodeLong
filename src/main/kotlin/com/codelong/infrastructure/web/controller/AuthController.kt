package com.codelong.infrastructure.web.controller

import com.codelong.application.command.LoginCommand
import com.codelong.application.command.RegisterUserCommand
import com.codelong.application.usecase.LoginUserUseCase
import com.codelong.application.usecase.RegisterUserUseCase
import com.codelong.infrastructure.web.dto.AuthResponse
import com.codelong.infrastructure.web.dto.LoginRequest
import com.codelong.infrastructure.web.dto.RegisterUserRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/auth"
private const val REGISTER_PATH = "/register"
private const val LOGIN_PATH = "/login"

@RestController
@RequestMapping(BASE_PATH)
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase
) {

    @PostMapping(REGISTER_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterUserRequest): AuthResponse =
        AuthResponse.from(
            registerUserUseCase.register(
                RegisterUserCommand(request.username, request.email, request.password)
            )
        )

    @PostMapping(LOGIN_PATH)
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        AuthResponse.from(
            loginUserUseCase.login(
                LoginCommand(request.identifier, request.password)
            )
        )
}