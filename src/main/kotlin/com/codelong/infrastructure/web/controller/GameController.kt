package com.codelong.infrastructure.web.controller

import com.codelong.application.command.AnswerQuestionCommand
import com.codelong.application.command.GameSearchQuery
import com.codelong.application.usecase.AbandonGameUseCase
import com.codelong.application.usecase.AnswerQuestionUseCase
import com.codelong.application.usecase.CreateGameUseCase
import com.codelong.application.usecase.GetCurrentQuestionUseCase
import com.codelong.application.usecase.GetGameUseCase
import com.codelong.application.usecase.GetInProgressGameUseCase
import com.codelong.application.usecase.ListGamesUseCase
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.AnswerRequest
import com.codelong.infrastructure.web.dto.AnswerResponse
import com.codelong.infrastructure.web.dto.GameResponse
import com.codelong.infrastructure.web.dto.PageResponse
import com.codelong.infrastructure.web.dto.PublicQuestionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games")
class GameController(
    private val createGameUseCase: CreateGameUseCase,
    private val getGameUseCase: GetGameUseCase,
    private val getCurrentQuestionUseCase: GetCurrentQuestionUseCase,
    private val answerQuestionUseCase: AnswerQuestionUseCase,
    private val abandonGameUseCase: AbandonGameUseCase,
    private val listGamesUseCase: ListGamesUseCase,
    private val getInProgressGameUseCase: GetInProgressGameUseCase
) {

    private val statusByCreation = mapOf(true to HttpStatus.CREATED, false to HttpStatus.OK)

    @PostMapping
    fun create(@AuthenticationPrincipal principal: AuthenticatedUser): ResponseEntity<GameResponse> {
        val result = createGameUseCase.create(principal.userId)
        return ResponseEntity.status(statusByCreation.getValue(result.created)).body(GameResponse.from(result.game))
    }

    @GetMapping
    fun history(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): PageResponse<GameResponse> {
        val query = GameSearchQuery(
            status = status?.let { GameStatus.fromName(it) },
            page = page,
            size = size
        )
        val result = listGamesUseCase.list(principal.userId, query)
        return PageResponse.of(
            items = result.items.map(GameResponse::from),
            totalElements = result.totalElements,
            page = result.page,
            size = result.size
        )
    }

    @GetMapping("/in-progress")
    fun inProgress(
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): ResponseEntity<GameResponse> {
        val game = getInProgressGameUseCase.current(principal.userId)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(GameResponse.from(game))
    }

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
