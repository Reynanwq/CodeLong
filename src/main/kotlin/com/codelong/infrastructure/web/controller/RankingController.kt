package com.codelong.infrastructure.web.controller

import com.codelong.application.usecase.GetMyRankingUseCase
import com.codelong.application.usecase.GetRankingUseCase
import com.codelong.domain.valueobject.GameMode
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.RankingEntryResponse
import com.codelong.infrastructure.web.dto.RankingResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/rankings"
private const val DEFAULT_PAGE = "0"
private const val DEFAULT_SIZE = "20"
private const val ME_PATH = "/me"

@RestController
@RequestMapping(BASE_PATH)
class RankingController(
    private val getRankingUseCase: GetRankingUseCase,
    private val getMyRankingUseCase: GetMyRankingUseCase
) {

    @GetMapping
    fun ranking(
        @RequestParam(required = false) mode: String?,
        @RequestParam(defaultValue = DEFAULT_PAGE) page: Int,
        @RequestParam(defaultValue = DEFAULT_SIZE) size: Int
    ): RankingResponse =
        RankingResponse.from(getRankingUseCase.ranking(page, size, mode?.let(GameMode::fromName)))

    @GetMapping(ME_PATH)
    fun me(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam(required = false) mode: String?
    ): ResponseEntity<RankingEntryResponse> {
        val entry = getMyRankingUseCase.myRanking(principal.userId, mode?.let(GameMode::fromName))
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(RankingEntryResponse.from(entry))
    }
}