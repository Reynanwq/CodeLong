package com.codelong.infrastructure.web.controller

import com.codelong.application.usecase.GetMyRankingUseCase
import com.codelong.application.usecase.GetRankingUseCase
import com.codelong.infrastructure.security.AuthenticatedUser
import com.codelong.infrastructure.web.dto.RankingEntryResponse
import com.codelong.infrastructure.web.dto.RankingResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rankings")
class RankingController(
    private val getRankingUseCase: GetRankingUseCase,
    private val getMyRankingUseCase: GetMyRankingUseCase
) {

    @GetMapping
    fun ranking(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RankingResponse =
        RankingResponse.from(getRankingUseCase.ranking(page, size))

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): ResponseEntity<RankingEntryResponse> {
        val entry = getMyRankingUseCase.myRanking(principal.userId)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(RankingEntryResponse.from(entry))
    }
}