package com.codelong.application.command

import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.OptionId

/**
 * O cliente informa somente a sua escolha. O resultado e a pontuacao sao
 * calculados exclusivamente no backend.
 */
data class AnswerQuestionCommand(
    val gameId: GameId,
    val optionId: OptionId
)