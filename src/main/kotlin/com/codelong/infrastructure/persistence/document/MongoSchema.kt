package com.codelong.infrastructure.persistence.document

/**
 * Vocabulario da persistencia MongoDB: colecoes, campos, indices e operadores
 * de agregacao. Nenhum nome de campo fica solto pelos adapters.
 */
object MongoSchema {

    object Collection {
        const val GAMES = "games"
        const val USERS = "users"
        const val QUESTIONS = "questions"
    }

    object Field {
        const val ID = "id"
        const val USER_ID = "userId"
        const val USERNAME = "username"
        const val STATUS = "status"
        const val ROLE = "role"
        const val SCORE = "score"
        const val CORRECT_ANSWERS = "correctAnswers"
        const val ANSWERS = "answers"
        const val ANSWERED_QUESTIONS = "answeredQuestions"
        const val STARTED_AT = "startedAt"
        const val COMPLETED_AT = "completedAt"
        const val CURRENT_QUESTION_DEADLINE = "currentQuestionDeadline"
        const val TIMED_OUT = "timedOut"
        const val CREATED_AT = "createdAt"
        const val CATEGORY = "category"
        const val DIFFICULTY = "difficulty"
        const val TOTAL_TIME_MILLIS = "totalTimeMillis"
        const val ACHIEVED_AT = "achievedAt"
    }

    object Index {
        const val GAME_RANKING = "game_ranking_idx"
        const val GAME_RANKING_DEF = "{'status': 1, 'score': -1, 'correctAnswers': -1}"
        const val GAME_USER = "game_user_idx"
        const val GAME_USER_DEF = "{'userId': 1, 'status': 1}"
        const val QUESTION_SEARCH = "question_search_idx"
        const val QUESTION_SEARCH_DEF = "{'status': 1, 'category': 1, 'difficulty': 1}"
    }

    object Operator {
        const val SUBTRACT = "\$subtract"
        const val COMPLETED_AT = "\$completedAt"
        const val STARTED_AT = "\$startedAt"
    }
}
