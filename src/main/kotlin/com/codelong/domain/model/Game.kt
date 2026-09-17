package com.codelong.domain.model

import com.codelong.domain.exception.DomainException
import com.codelong.domain.valueobject.AnswerEval
import com.codelong.domain.valueobject.AnswerRecord
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameQuestion
import com.codelong.domain.valueobject.GameSetup
import com.codelong.domain.valueobject.GameState
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

    fun totalQuestions(): Int = questions.size

    fun isInProgress(): Boolean = status == GameStatus.IN_PROGRESS

    fun isCompleted(): Boolean = status == GameStatus.COMPLETED

    fun isOwnedBy(actor: UserId): Boolean = userId == actor

    fun requireOwner(actor: UserId) {
        if (!isOwnedBy(actor)) {
            throw DomainException.forbidden("GAME_ACCESS_DENIED", "You do not have access to this game")
        }
    }

    fun currentQuestion(): GameQuestion {
        requireInProgress()
        return questions[currentQuestionIndex]
    }

    /** Quantidade de perguntas restantes incluindo a atual. */
    fun remainingQuestions(): Int = questions.size - currentQuestionIndex

    fun answered(questionIndex: Int): Boolean = answers.any { it.questionIndex == questionIndex }

    /**
     * Processa uma resposta para a pergunta atual.
     *
     * O avanco de [currentQuestionIndex] e a contabilizacao da pontuacao
     * acontecem aqui, dentro do dominio. Duas requisicoes concorrentes que
     * leiam o mesmo estado so persistem se uma delas vencer o optimistic lock
     * (campo [version]); a perdedora e rejeitada na persistencia.
     */
    fun answer(optionId: OptionId, answeredAt: Instant): AnswerEval {
        requireInProgress()

        val question = questions[currentQuestionIndex]
        if (!question.hasOption(optionId)) {
            throw DomainException.invalidInput(
                "answer.option.invalid",
                "The chosen option is not valid for the current question"
            )
        }

        val correct = question.isCorrect(optionId)
        val earnedPoints = if (correct) question.difficulty.points else 0
        answers.add(
            AnswerRecord(
                questionIndex = currentQuestionIndex,
                questionId = question.id,
                chosenOption = optionId,
                correct = correct,
                earnedPoints = earnedPoints,
                answeredAt = answeredAt
            )
        )

        score += earnedPoints
        if (correct) correctAnswers++ else wrongAnswers++

        val answeredIndex = currentQuestionIndex
        val isLast = answeredIndex >= questions.size - 1
        currentQuestionIndex++

        if (isLast) {
            status = GameStatus.COMPLETED
            completedAt = answeredAt
        }

        return AnswerEval(
            record = answers.last(),
            question = question,
            currentScore = score,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            gameCompleted = status == GameStatus.COMPLETED,
            questionIndex = answeredIndex,
            totalQuestions = questions.size
        )
    }

    fun abandon(now: Instant) {
        requireInProgress()
        status = GameStatus.ABANDONED
        completedAt = now
    }

    fun state(): GameState = GameState(
        id = id,
        userId = userId,
        username = username,
        status = status,
        startedAt = startedAt,
        completedAt = completedAt,
        currentQuestionIndex = currentQuestionIndex,
        questions = questions,
        answers = answers.toList(),
        score = score,
        correctAnswers = correctAnswers,
        wrongAnswers = wrongAnswers,
        version = version
    )

    private fun requireInProgress() {
        if (status != GameStatus.IN_PROGRESS) {
            throw DomainException.conflict(
                "GAME_FINISHED",
                "This game is already finished and cannot receive new answers"
            )
        }
    }

    companion object {
        fun newGame(id: GameId, setup: GameSetup, startedAt: Instant): Game = Game(
            id = id,
            userId = setup.userId,
            username = setup.username,
            status = GameStatus.IN_PROGRESS,
            startedAt = startedAt,
            completedAt = null,
            currentQuestionIndex = 0,
            questions = setup.questions,
            answers = mutableListOf(),
            score = 0,
            correctAnswers = 0,
            wrongAnswers = 0,
            version = 0L
        )

        fun reconstitute(state: GameState): Game = Game(
            id = state.id,
            userId = state.userId,
            username = state.username,
            status = state.status,
            startedAt = state.startedAt,
            completedAt = state.completedAt,
            currentQuestionIndex = state.currentQuestionIndex,
            questions = state.questions,
            answers = state.answers.toMutableList(),
            score = state.score,
            correctAnswers = state.correctAnswers,
            wrongAnswers = state.wrongAnswers,
            version = state.version
        )
    }
}