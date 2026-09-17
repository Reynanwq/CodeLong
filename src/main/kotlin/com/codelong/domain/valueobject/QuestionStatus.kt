package com.codelong.domain.valueobject

enum class QuestionStatus {
    ACTIVE,
    INACTIVE;

    companion object {

        /** Converte a flag de ativacao no status correspondente, sem ramificacao. */
        fun of(active: Boolean): QuestionStatus = mapOf(true to ACTIVE, false to INACTIVE).getValue(active)
    }
}