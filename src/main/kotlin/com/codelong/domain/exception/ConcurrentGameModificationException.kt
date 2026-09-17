package com.codelong.domain.exception

/**
 * Lancada quando uma partida foi modificada concorrentemente por outra
 * requisicao. Mapeada para HTTP 409.
 */
class ConcurrentGameModificationException(
    code: String = "CONCURRENT_MODIFICATION",
    message: String = "The game was modified concurrently. Reload the current state and try again."
) : DomainException(code, message)