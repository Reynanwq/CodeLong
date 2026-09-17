package com.codelong.infrastructure.web.dto

import java.time.Instant

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant
) {
    companion object {
        fun of(code: String, message: String) = ApiErrorResponse(code, message, Instant.now())
    }
}

data class PageResponse<T>(
    val items: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        fun <T> of(items: List<T>, totalElements: Long, page: Int, size: Int): PageResponse<T> {
            val totalPages = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
            return PageResponse(items, totalElements, page, size, totalPages)
        }
    }
}