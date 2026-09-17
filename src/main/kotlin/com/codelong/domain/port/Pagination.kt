package com.codelong.domain.port

/**
 * Regras de paginacao compartilhadas pelos filtros de busca, evitando que os
 * limites e as mensagens fiquem duplicados ou soltos pelo codigo.
 */
object Pagination {

    const val MIN_PAGE = 0
    const val MIN_SIZE = 1
    const val MAX_SIZE = 100
    const val PAGE_MESSAGE = "page must be >= 0"
    const val SIZE_MESSAGE = "size must be between $MIN_SIZE and $MAX_SIZE"

    fun requireValid(page: Int, size: Int) {
        require(page >= MIN_PAGE) { PAGE_MESSAGE }
        require(size in MIN_SIZE..MAX_SIZE) { SIZE_MESSAGE }
    }
}
