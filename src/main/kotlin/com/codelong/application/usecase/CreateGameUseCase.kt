package com.codelong.application.usecase

import com.codelong.application.service.GameFactory
import com.codelong.domain.exception.ConflictException
import com.codelong.domain.exception.ForbiddenException
import com.codelong.domain.exception.NotFoundException
import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.UserRepository
import com.codelong.domain.valueobject.UserId

class CreateGameUseCase(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val gameRepository: GameRepository,
    private val gameFactory: GameFactory
) {

    fun create(actorId: UserId): Game {
        val user = userRepository.findById(actorId)
            ?: throw NotFoundException("USER_NOT_FOUND", "User not found")

        if (!user.isActive()) {
            throw ForbiddenException("ACCOUNT_INACTIVE", "This account is not active")
        }

        val activeQuestions = questionRepository.findAllActive()
        if (activeQuestions.isEmpty()) {
            throw ConflictException(
                "NO_ACTIVE_QUESTIONS",
                "There are no active questions available to start a game"
            )
        }

        return gameRepository.save(gameFactory.start(user, activeQuestions))
    }
}