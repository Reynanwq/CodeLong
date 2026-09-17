package com.codelong.application.usecase

import com.codelong.application.command.AnswerQuestionCommand
import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.AnswerResult
import com.codelong.domain.valueobject.UserId
import java.time.Clock

class AnswerQuestionUseCase(
    private val gameRepository: GameRepository,
    private val clock: Clock
) {

    fun answer(command: AnswerQuestionCommand, actorId: UserId): AnswerResult {
        val game = gameRepository.findById(command.gameId)
            ?: throw NotFoundException("GAME_NOT_FOUND", "Game not found")
        game.requireOwner(actorId)

        val evaluation = game.answer(command.optionId, clock.instant())

        val saved = gameRepository.save(game)
        val nextQuestion = if (saved.isInProgress()) saved.currentQuestion().publicView() else null

        return AnswerResult(
            record = evaluation.record,
            question = evaluation.question,
            currentScore = saved.score(),
            correctAnswers = saved.correctAnswersCount(),
            wrongAnswers = saved.wrongAnswersCount(),
            gameCompleted = saved.isCompleted(),
            questionIndex = evaluation.questionIndex,
            totalQuestions = evaluation.totalQuestions,
            nextQuestion = nextQuestion
        )
    }
}