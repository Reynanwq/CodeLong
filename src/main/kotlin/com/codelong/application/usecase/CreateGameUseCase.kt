package com.codelong.application.usecase

import com.codelong.application.result.GameCreationResult
import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.Errors
import com.codelong.domain.model.Question
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.UserId

/**
 * Abre uma partida para o usuario no modo informado.
 *
 * Se ja existe uma partida em andamento, ela e retomada em vez de criar uma
 * segunda — o jogador nunca fica com duas partidas abertas ao mesmo tempo.
 *
 * No modo [GameMode.APRENDIZADO] e obrigatorio informar um tema ([category]) ou
 * uma pergunta especifica ([questionId]); os demais modos usam todas as
 * perguntas ativas.
 */
interface CreateGameUseCase {
    fun create(
        actorId: UserId,
        mode: GameMode,
        category: Category?,
        questionId: QuestionId?
    ): GameCreationResult
}

class CreateGameUseCaseImpl(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val gameRepository: GameRepository,
    private val gameFactory: GameFactory
) : CreateGameUseCase {

    override fun create(
        actorId: UserId,
        mode: GameMode,
        category: Category?,
        questionId: QuestionId?
    ): GameCreationResult {
        val user = userRepository.findById(actorId)
            ?: throw Errors.userNotFound()

        user.isActive.takeUnless { it }?.let {
            throw Errors.accountInactiveForbidden()
        }

        gameRepository.findInProgressByUserId(actorId)?.let {
            return GameCreationResult(game = it, created = false)
        }

        val questions = questionsFor(mode, category, questionId)
        questions.isEmpty().takeIf { it }?.let {
            throw Errors.noActiveQuestions()
        }

        val game = gameRepository.save(gameFactory.start(user, questions, mode))
        return GameCreationResult(game = game, created = true)
    }

    private fun questionsFor(mode: GameMode, category: Category?, questionId: QuestionId?): List<Question> =
        when (mode) {
            GameMode.APRENDIZADO -> learningQuestions(category, questionId)
            GameMode.GUBEE -> questionRepository.findActiveByCategory(Category.GUBEE)
            GameMode.CLASSIC, GameMode.GENOCIDA -> questionRepository.findAllActive()
                .filterNot { it.category == Category.GUBEE }
        }

    /** Um tema inteiro (dificuldade crescente) ou uma unica pergunta do tema. */
    private fun learningQuestions(category: Category?, questionId: QuestionId?): List<Question> {
        questionId?.let { id ->
            val question = questionRepository.findById(id) ?: throw Errors.questionNotFound()
            question.isActive.takeUnless { it }?.let { throw Errors.noActiveQuestions() }
            question.category.takeIf { it == Category.GUBEE }?.let { throw Errors.noActiveQuestions() }
            return listOf(question)
        }
        val theme = category ?: throw Errors.learningThemeRequired()
        theme.takeIf { it == Category.GUBEE }?.let { throw Errors.noActiveQuestions() }
        return questionRepository.findActiveByCategory(theme)
    }
}
