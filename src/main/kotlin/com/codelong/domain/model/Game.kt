package com.codelong.domain.model

import com.codelong.domain.exception.ConflictException
import com.codelong.domain.exception.ForbiddenException
import com.codelong.domain.exception.InvalidInputException
import com.codelong.domain.valueobject.AnswerEval
import com.codelong.domain.valueobject.AnswerRecord
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameQuestion
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.OptionId
import com.codelong.domain.valueobject.UserId
import java.time.Instant

class Game private constructor(
    val id: GameId,
    val userId: UserId,
    val username: String,
    private var status: GameStatus,
    val startedAt: Instant,
    private var completedAt: Instant?,
    private var currentQuestionIndex: Int,
    private val questions: List<GameQuestion>,
    private val answers: MutableList<AnswerRecord>,
    private var score: Int,
    private var correctAnswers: Int,
    private var wrongAnswers: Int,
    var version: Long
) {

    fun status(): GameStatus = status

    fun completedAt(): Instant? = completedAt

    fun currentQuestionIndex(): Int = currentQuestionIndex

    fun questions(): List<GameQuestion> = questions

    fun answers(): List<AnswerRecord> = answers.toList()

    fun score(): Int = score

    fun correctAnswersCount(): Int = correctAnswers

    fun wrongAnswersCount(): Int = wrongAnswers

    fun isFinished(): Boolean = status != GameStatus.IN_PROGRESS

    fun isInProgress(): Boolean = status == GameStatus.IN_PROGRESS

    fun isOwnedBy(actor: UserId): Boolean = userId == actor

    fun requireOwner(actor: UserId) {
        if (!isOwnedBy(actor)) {
            throw ForbiddenException(
                "GAME_ACCESS_DENIED",
                "You do not have access to this game"
            )
        }
    }

    private fun requireInProgress() {
        if (status != GameStatus.IN_PROGRESS) {
            throw ConflictException(
                "GAME_FINISHED",
                "This game is already finished and cannot receive new answers"
            )
        }
    }

    fun currentQuestion(): GameQuestion {
        requireInProgress()
        return questions[currentQuestionIndex]
    }

    /** Quantidade de perguntas restantes incluindo a atual. */
    fun remainingQuestions(): Int = questions.size - currentQuestionIndex

    /**
     * Processa uma resposta para a pergunta atual.
     *
     * Regra de concorrencia/duplicidade: o avanco de [currentQuestionIndex]
     * acontece dentro deste metodo. Duas requisicoes concorrentes que leiam o
     * mesmo estado persistem apenas se uma delas venceu o optimistic lock
     * (campo [version]); a perdedora e rejeitada na persistencia.
     */
    fun answer(optionId: OptionId, answeredAt: Instant): AnswerEval {
        requireInProgress()

        val question = questions[currentQuestionIndex]
        if (!question.hasOption(optionId)) {
            throw InvalidInputException(
                "answer.option.invalid",
                "The chosen option is not valid for the current question"
            )
        }

        val correct = question.isCorrect(optionId)
        val earnedPoints = if (correct) question.difficulty.points else 0
        val record = AnswerRecord(
            questionIndex = currentQuestionIndex,
            questionId = question.id,
            chosenOption = optionId,
            correct = correct,
            earnedPoints = earnedPoints,
            answeredAt = answeredAt
        )
        answers.add(record)

        score += earnedPoints
        if (correct) correctAnswers++ else wrongAnswers++

        val answerIndex = currentQuestionIndex
        val isLast = answerIndex >= questions.size - 1
        currentQuestionIndex++

        if (isLast) {
            status = GameStatus.COMPLETED
            completedAt = answeredAt
        }

        return AnswerEval(
            record = record,
            question = question,
            currentScore = score,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            gameCompleted = status == GameStatus.COMPLETED,
            questionIndex = answerIndex,
            totalQuestions = questions.size
        )
    }

    fun abandon(now: Instant) {
        requireInProgress()
        status = GameStatus.ABANDONED
        completedAt = now
    }

    fun answered(questionIndex: Int): Boolean = answers.any { it.questionIndex == questionIndex }

    companion object {
        fun newGame(
            id: GameId,
            userId: UserId,
            username: String,
            questions: List<GameQuestion>,
            startedAt: Instant
        ): Game = Game(
            id = id,
            userId = userId,
            username = username,
            status = GameStatus.IN_PROGRESS,
            startedAt = startedAt,
            completedAt = null,
            currentQuestionIndex = 0,
            questions = questions,
            answers = mutableListOf(),
            score = 0,
            correctAnswers = 0,
            wrongAnswers = 0,
            version = 0L
        )
    }
}