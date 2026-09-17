package com.codelong.application.result

import com.codelong.domain.valueobject.RankEntry

data class RankingPage(
    val entries: List<RankEntry>,
    val totalElements: Long,
    val page: Int,
    val size: Int
)