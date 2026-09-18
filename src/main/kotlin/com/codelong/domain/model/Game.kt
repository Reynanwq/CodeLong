package com.codelong.domain.model

import com.codelong.domain.GameRules
import com.codelong.domain.exception.Errors
import com.codelong.domain.valueobject.AnswerEval
import com.codelong.domain.valueobject.AnswerRecord
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameMode
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
    val mode: GameMode,
    status: GameStatus,
    val startedAt: Instant,
    completedAt: Instant?,
    currentQuestionIndex: Int,
    currentQuestionDeadline: Instant,
    val questions: List<GameQuestion>,
    answerRecords: MutableList<AnswerRecord>,
    score: Int,
    correctAnswerCount: Int,
    wrongAnswerCount: Int,
    var version: Long
) {

    var status: GameStatus = status
        private set

    var completedAt: Instant? = completedAt
        private set

    var currentQuestionIndex: Int = currentQuestionIndex
        private set

    /** Instante limite para responder a pergunta atual. */
    var currentQuestionDeadline: Instant = currentQuestionDeadline
        private set

    var score: Int = score
        private set

    var correctAnswers: Int = correctAnswerCount
        private set

    var wrongAnswers: Int = wrongAnswerCount
        private set

    private val answerRecords: MutableList<AnswerRecord> = answerRecords

    val answers: List<AnswerRecord> get() = answerRecords.toList()

    val totalQuestions: Int get() = questions.size

    /** Quantidade de perguntas restantes incluindo a atual. */
    val remainingQuestions: Int get() = questions.size - currentQuestionIndex

    val isInProgress: Boolean get() = status == GameStatus.IN_PROGRESS

    val isCompleted: Boolean get() = status == GameStatus.COMPLETED

    val idText: String get() = id.value

    val statusName: String get() = status.name

    fun isOwnedBy(actor: UserId): Boolean = userId == actor

    fun requireOwner(actor: UserId) {
        isOwnedBy(actor).takeUnless { it }?.let {
            throw Errors.gameAccessDenied()
        }
    }

    fun currentQuestion(): GameQuestion {
        requireInProgress()
        return questions[currentQuestionIndex]
    }

    /** Indica se o prazo da pergunta atual ja passou. */
    fun isCurrentQuestionExpired(now: Instant): Boolean = now.isAfter(currentQuestionDeadline)

    fun answered(questionIndex: Int): Boolean = answerRecords.any { it.questionIndex == questionIndex }

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

        isCurrentQuestionExpired(answeredAt).takeIf { it }?.let {
            throw Errors.answerTimeExpired()
        }

        val question = questions[currentQuestionIndex]
        question.hasOption(optionId).takeUnless { it }?.let {
            throw Errors.invalidAnswerOption()
        }

        return record(question, optionId, question.isCorrect(optionId), answeredAt)
    }

    /**
     * Contabiliza a pergunta atual como perdida por tempo e avanca para a
     * proxima, sem pontuar.
     */
    fun expireCurrentQuestion(now: Instant): AnswerEval {
        requireInProgress()
        return record(questions[currentQuestionIndex], chosenOption = null, correct = false, answeredAt = now)
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
        mode = mode,
        status = status,
        startedAt = startedAt,
        completedAt = completedAt,
        currentQuestionIndex = currentQuestionIndex,
        currentQuestionDeadline = currentQuestionDeadline,
        questions = questions,
        answers = answers,
        score = score,
        correctAnswers = correctAnswers,
        wrongAnswers = wrongAnswers,
        version = version
    )

    private fun record(
        question: GameQuestion,
        chosenOption: OptionId?,
        correct: Boolean,
        answeredAt: Instant
    ): AnswerEval {
        val earnedPoints = question.pointsForCorrect.takeIf { correct } ?: 0
        answerRecords.add(
            AnswerRecord(
                questionIndex = currentQuestionIndex,
                questionId = question.id,
                chosenOption = chosenOption,
                correct = correct,
                earnedPoints = earnedPoints,
                answeredAt = answeredAt,
                timedOut = chosenOption == null
            )
        )

        score += earnedPoints
        val increments = mapOf(true to 1, false to 0)
        correctAnswers += increments.getValue(correct)
        wrongAnswers += increments.getValue(!correct)

        val answeredIndex = currentQuestionIndex
        val isLast = answeredIndex >= questions.size - 1
        currentQuestionIndex++

        isLast.takeIf { it }?.let {
            status = GameStatus.COMPLETED
            completedAt = answeredAt
        }
        currentQuestionDeadline = answeredAt.plus(GameRules.ANSWER_TIME_LIMIT)

        return AnswerEval(
            record = answerRecords.last(),
            question = question,
            currentScore = score,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            gameCompleted = isCompleted,
            questionIndex = answeredIndex,
            totalQuestions = questions.size
        )
    }

    private fun requireInProgress() {
        status.takeUnless { it == GameStatus.IN_PROGRESS }?.let {
            throw Errors.gameFinished()
        }
    }

    companion object {
        fun newGame(id: GameId, setup: GameSetup, startedAt: Instant): Game = Game(
            id = id,
            userId = setup.userId,
            username = setup.username,
            mode = setup.mode,
            status = GameStatus.IN_PROGRESS,
            startedAt = startedAt,
            completedAt = null,
            currentQuestionIndex = 0,
            currentQuestionDeadline = startedAt.plus(GameRules.ANSWER_TIME_LIMIT),
            questions = setup.questions,
            answerRecords = mutableListOf(),
            score = 0,
            correctAnswerCount = 0,
            wrongAnswerCount = 0,
            version = 0L
        )

        fun reconstitute(state: GameState): Game = Game(
            id = state.id,
            userId = state.userId,
            username = state.username,
            mode = state.mode,
            status = state.status,
            startedAt = state.startedAt,
            completedAt = state.completedAt,
            currentQuestionIndex = state.currentQuestionIndex,
            currentQuestionDeadline = state.currentQuestionDeadline,
            questions = state.questions,
            answerRecords = state.answers.toMutableList(),
            score = state.score,
            correctAnswerCount = state.correctAnswers,
            wrongAnswerCount = state.wrongAnswers,
            version = state.version
        )
    }
}
