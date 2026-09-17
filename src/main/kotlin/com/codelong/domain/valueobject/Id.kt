package com.codelong.domain.valueobject

import java.util.UUID

@JvmInline
value class UserId(val value: String)

@JvmInline
value class QuestionId(val value: String)

@JvmInline
value class GameId(val value: String)

@JvmInline
value class OptionId(val value: String)

object Ids {
    fun newUserId(): UserId = UserId(UUID.randomUUID().toString())
    fun newQuestionId(): QuestionId = QuestionId(UUID.randomUUID().toString())
    fun newGameId(): GameId = GameId(UUID.randomUUID().toString())
}