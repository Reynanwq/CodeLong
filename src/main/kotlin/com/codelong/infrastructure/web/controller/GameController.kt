package com.codelong.infrastructure.web.controller

import com.codelong.application.command.AnswerQuestionCommand
import com.codelong.application.usecase.AbandonGameUseCase
import com.codelong.application.usecase.AnswerQuestionUseCase
import com.codelong.application.usecase.CreateGameUseCase
import com.codelong.application.usecase.GetCurrentQuestionUseCase
import com.codelong.application.usecase.GetGameUseCase
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.OptionId
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.AnswerRequest
import com.codelong.infrastructure.web.dto.AnswerResponse
import com.codelong.infrastructure.web.dto.GameResponse
import com.codelong.infrastructure.web.dto.PublicQuestionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games")
class GameController(
    private val createGameUseCase: CreateGameUseCase,
    private val getGameUseCase: GetGameUseCase,
    private val getCurrentQuestionUseCase: GetCurrentQuestionUseCase,
    private val answerQuestionUseCase: AnswerQuestionUseCase,
    private val abandonGameUseCase: AbandonGameUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal principal: AuthenticatedUser): GameResponse =
        GameResponse.from(createGameUseCase.create(principal.userId))

    @GetMapping("/{gameId}")
    fun get(
        @PathVariable gameId: String,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): GameResponse =
        GameResponse.from(getGameUseCase.get(GameId(gameId), principal.userId))

    @GetMapping("/{gameId}/current-question")
    fun currentQuestion(
        @PathVariable gameId: String,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): PublicQuestionResponse =
        PublicQuestionResponse.from(getCurrentQuestionUseCase.current(GameId(gameId), principal.userId))

    @PostMapping("/{gameId}/answers")
    fun answer(
        @PathVariable gameId: String,
        @Valid @RequestBody request: AnswerRequest,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): AnswerResponse =
        AnswerResponse.from(
            answerQuestionUseCase.answer(
                AnswerQuestionCommand(GameId(gameId), OptionId(request.optionId.trim())),
                principal.userId
            )
        )

    @PostMapping("/{gameId}/abandon")
    fun abandon(
        @PathVariable gameId: String,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): GameResponse =
        GameResponse.from(abandonGameUseCase.abandon(GameId(gameId), principal.userId))
}