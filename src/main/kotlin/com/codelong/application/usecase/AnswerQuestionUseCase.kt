package com.codelong.application.usecase

import com.codelong.domain.exception.Errors


import com.codelong.application.command.AnswerQuestionCommand
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.AnswerResult
import com.codelong.domain.valueobject.UserId
import java.time.Clock

interface AnswerQuestionUseCase {
    fun answer(command: AnswerQuestionCommand, actorId: UserId): AnswerResult
}


class AnswerQuestionUseCaseImpl(
    private val gameRepository: GameRepository,
    private val clock: Clock
) : AnswerQuestionUseCase {


    override fun answer(command: AnswerQuestionCommand, actorId: UserId): AnswerResult {
        val game = gameRepository.findById(command.gameId)
            ?: throw Errors.gameNotFound()
        game.requireOwner(actorId)

        val evaluation = game.answer(command.optionId, clock.instant())

        val saved = gameRepository.save(game)
        val nextQuestion = saved.takeIf { it.isInProgress }?.currentQuestion()?.publicView()

        return AnswerResult(
            record = evaluation.record,
            question = evaluation.question,
            currentScore = saved.score,
            correctAnswers = saved.correctAnswers,
            wrongAnswers = saved.wrongAnswers,
            gameCompleted = saved.isCompleted,
            questionIndex = evaluation.questionIndex,
            totalQuestions = evaluation.totalQuestions,
            nextQuestion = nextQuestion
        )
    }
}