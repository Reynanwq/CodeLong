package com.codelong.infrastructure.persistence.document

/**
 * Alternativa de resposta persistida (embutida em perguntas e em snapshots de
 * partida).
 */
data class OptionDocument(
    val id: String = "",
    val text: String = ""
)