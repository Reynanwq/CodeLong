package com.codelong.domain.valueobject

import com.codelong.domain.exception.Errors

/**
 * Categorias de conhecimento cobertas pelas perguntas.
 *
 * Novas categorias podem ser adicionadas com novos valores deste enum sem
 * alterar as regras de negocio existentes.
 */
enum class Category {
    JAVA,
    KOTLIN,
    SPRING,
    OOP,
    SOLID,
    CLEAN_CODE,
    DESIGN_PATTERNS,
    SOFTWARE_ARCHITECTURE,
    TESTING,
    DATABASE,
    REST,
    MICROSERVICES,
    GIT,
    DOCKER,
    KAFKA,
    SYSTEM_DESIGN,
    COMPUTING_HISTORY,
    AWS,
    SECURITY,
    ACRONYMS,
    OBSERVABILITY,
    CONCURRENCY,
    RESILIENCE,
    EVENT_DRIVEN,
    AI,
    CLAUDE_CODE,
    ALGORITHMS;

    companion object {
        fun fromName(name: String): Category =
            entries.firstOrNull { it.name == name.uppercase() }
                ?: throw Errors.unknownCategory(name)
    }
}
