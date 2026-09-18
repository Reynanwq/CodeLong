package com.codelong.domain.valueobject

import kotlin.random.Random

/**
 * Snapshot da pergunta persistido na partida no momento de sua criacao.
 *
 * O snapshot garante que a partida permaneça consistente mesmo que a pergunta
 * original seja editada, desativada ou removida. A resposta correta e a
 * explicacao ficam preservadas internamente para conferencia/resultado, mas
 * jamais sao expostas ao cliente durante a partida.
 */
data class GameQuestion(
    val id: QuestionId,
    val statement: String,
    val options: List<QuestionOption>,
    val correctOption: OptionId,
    val explanation: String,
    val category: Category,
    val difficulty: Difficulty
) {
    fun isCorrect(optionId: OptionId): Boolean = correctOption == optionId

    fun hasOption(optionId: OptionId): Boolean = options.any { it.id == optionId }

    val idText: String get() = id.value

    val correctOptionText: String get() = correctOption.value

    val pointsForCorrect: Int get() = difficulty.points

    val categoryName: String get() = category.name

    val difficultyName: String get() = difficulty.name

    /** Devolve o snapshot com as alternativas em ordem aleatoria. */
    fun withShuffledOptions(random: Random): GameQuestion = copy(options = options.shuffled(random))

    /** Visao publica: sem a resposta correta e sem a explicacao. */
    fun publicView(): QuestionPublic {
        return QuestionPublic(
            id = id,
            statement = statement,
            options = options,
            category = category,
            difficulty = difficulty
        )
    }
}

data class QuestionPublic(
    val id: QuestionId,
    val statement: String,
    val options: List<QuestionOption>,
    val category: Category,
    val difficulty: Difficulty
) {
    val idText: String get() = id.value

    val categoryName: String get() = category.name

    val difficultyName: String get() = difficulty.name
}