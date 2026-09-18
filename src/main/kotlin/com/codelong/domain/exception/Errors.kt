package com.codelong.domain.exception

/**
 * Catalogo unico do vocabulario de erros de dominio.
 *
 * Nenhum codigo ou mensagem de erro fica solto pelo codigo: este objeto e a
 * unica fonte de verdade, garantindo consistencia entre dominio, aplicacao e
 * borda HTTP.
 */
object Errors {

    // --- Codigos estaveis (contrato com o cliente) ---
    const val EMAIL_INVALID = "email.invalid"
    const val USERNAME_INVALID = "username.invalid"
    const val STATUS_INVALID = "status.invalid"
    const val ROLE_INVALID = "role.invalid"
    const val CATEGORY_INVALID = "category.invalid"
    const val DIFFICULTY_INVALID = "difficulty.invalid"
    const val QUESTION_STATEMENT_INVALID = "question.statement.invalid"
    const val QUESTION_OPTIONS_INVALID = "question.options.invalid"
    const val QUESTION_CORRECT_OPTION_INVALID = "question.correctOption.invalid"
    const val QUESTION_EXPLANATION_INVALID = "question.explanation.invalid"
    const val ANSWER_OPTION_INVALID = "answer.option.invalid"
    const val PASSWORD_TOO_WEAK = "password.tooWeak"
    const val PASSWORD_UNCHANGED = "password.unchanged"
    const val USER_DEACTIVATE_SELF = "user.deactivate.self"
    const val PAGINATION_PAGE_INVALID = "pagination.page.invalid"
    const val PAGINATION_SIZE_INVALID = "pagination.size.invalid"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE"
    const val INVALID_CURRENT_PASSWORD = "INVALID_CURRENT_PASSWORD"
    const val TOKEN_INVALID = "TOKEN_INVALID"
    const val GAME_ACCESS_DENIED = "GAME_ACCESS_DENIED"
    const val USER_NOT_FOUND = "USER_NOT_FOUND"
    const val GAME_NOT_FOUND = "GAME_NOT_FOUND"
    const val QUESTION_NOT_FOUND = "QUESTION_NOT_FOUND"
    const val USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS"
    const val EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS"
    const val GAME_FINISHED = "GAME_FINISHED"
    const val NO_ACTIVE_QUESTIONS = "NO_ACTIVE_QUESTIONS"
    const val ANSWER_TIME_EXPIRED = "ANSWER_TIME_EXPIRED"
    const val GAME_MODE_INVALID = "game.mode.invalid"

    // --- Fabricas de falha ---
    fun emailInvalid(): DomainException =
        DomainException.invalidInput(EMAIL_INVALID, "The email address is invalid")

    fun usernameInvalid(): DomainException = DomainException.invalidInput(
        USERNAME_INVALID,
        "Username must have between 3 and 20 characters using only letters, numbers, '_' or '-'"
    )

    fun unknownAccountStatus(name: String): DomainException =
        DomainException.invalidInput(STATUS_INVALID, "Unknown account status: $name")

    fun unknownGameStatus(name: String): DomainException =
        DomainException.invalidInput(STATUS_INVALID, "Unknown game status: $name")

    fun unknownRole(name: String): DomainException =
        DomainException.invalidInput(ROLE_INVALID, "Unknown role: $name")

    fun unknownCategory(name: String): DomainException =
        DomainException.invalidInput(CATEGORY_INVALID, "Unknown category: $name")

    fun unknownDifficultyLevel(minLevel: Int, maxLevel: Int): DomainException =
        DomainException.invalidInput(DIFFICULTY_INVALID, "Difficulty level must be between $minLevel and $maxLevel")

    fun unknownDifficulty(value: String): DomainException =
        DomainException.invalidInput(DIFFICULTY_INVALID, "Unknown difficulty: $value")

    fun statementInvalid(maxStatement: Int): DomainException = DomainException.invalidInput(
        QUESTION_STATEMENT_INVALID,
        "Statement must not be blank and at most $maxStatement characters"
    )

    fun tooFewOptions(minOptions: Int): DomainException =
        DomainException.invalidInput(QUESTION_OPTIONS_INVALID, "A question must have at least $minOptions options")

    fun blankOptionText(): DomainException =
        DomainException.invalidInput(QUESTION_OPTIONS_INVALID, "Option text must not be blank")

    fun duplicatedOptionIds(): DomainException =
        DomainException.invalidInput(QUESTION_OPTIONS_INVALID, "Option ids must be unique")

    fun correctOptionNotInOptions(): DomainException =
        DomainException.invalidInput(QUESTION_CORRECT_OPTION_INVALID, "The correct option must be one of the options")

    fun explanationInvalid(maxExplanation: Int): DomainException = DomainException.invalidInput(
        QUESTION_EXPLANATION_INVALID,
        "Explanation must not be blank and at most $maxExplanation characters"
    )

    fun invalidAnswerOption(): DomainException =
        DomainException.invalidInput(ANSWER_OPTION_INVALID, "The chosen option is not valid for the current question")

    fun passwordTooShort(minLength: Int): DomainException =
        DomainException.invalidInput(PASSWORD_TOO_WEAK, "Password must have at least $minLength characters")

    fun passwordTooLong(maxLength: Int): DomainException =
        DomainException.invalidInput(PASSWORD_TOO_WEAK, "Password must have at most $maxLength characters")

    fun passwordUnchanged(): DomainException =
        DomainException.invalidInput(PASSWORD_UNCHANGED, "The new password must differ from the current one")

    fun selfDeactivation(): DomainException =
        DomainException.invalidInput(USER_DEACTIVATE_SELF, "An administrator cannot deactivate their own account")

    fun invalidPage(): DomainException =
        DomainException.invalidInput(PAGINATION_PAGE_INVALID, "Page must be greater than or equal to 0")

    fun invalidPageSize(minSize: Int, maxSize: Int): DomainException =
        DomainException.invalidInput(PAGINATION_SIZE_INVALID, "Page size must be between $minSize and $maxSize")

    fun invalidCredentials(): DomainException =
        DomainException.unauthorized(INVALID_CREDENTIALS, "Invalid credentials")

    fun accountInactiveUnauthorized(): DomainException =
        DomainException.unauthorized(ACCOUNT_INACTIVE, "This account is not active")

    fun invalidCurrentPassword(): DomainException =
        DomainException.unauthorized(INVALID_CURRENT_PASSWORD, "The current password is incorrect")

    fun tokenWithoutSubject(): DomainException =
        DomainException.unauthorized(TOKEN_INVALID, "The token is missing its subject")

    fun tokenWithoutRole(): DomainException =
        DomainException.unauthorized(TOKEN_INVALID, "The token is missing its role")

    fun gameAccessDenied(): DomainException =
        DomainException.forbidden(GAME_ACCESS_DENIED, "You do not have access to this game")

    fun accountInactiveForbidden(): DomainException =
        DomainException.forbidden(ACCOUNT_INACTIVE, "This account is not active")

    fun userNotFound(): DomainException =
        DomainException.notFound(USER_NOT_FOUND, "User not found")

    fun gameNotFound(): DomainException =
        DomainException.notFound(GAME_NOT_FOUND, "Game not found")

    fun questionNotFound(): DomainException =
        DomainException.notFound(QUESTION_NOT_FOUND, "Question not found")

    fun usernameAlreadyExists(): DomainException =
        DomainException.conflict(USERNAME_ALREADY_EXISTS, "This username is already taken")

    fun emailAlreadyExists(): DomainException =
        DomainException.conflict(EMAIL_ALREADY_EXISTS, "This email is already registered")

    fun gameFinished(): DomainException =
        DomainException.conflict(GAME_FINISHED, "This game is already finished and cannot receive new answers")

    fun answerTimeExpired(): DomainException =
        DomainException.conflict(ANSWER_TIME_EXPIRED, "The time to answer the current question has expired")

    fun unknownGameMode(name: String): DomainException =
        DomainException.invalidInput(GAME_MODE_INVALID, "Unknown game mode: $name")

    fun noActiveQuestions(): DomainException =
        DomainException.conflict(NO_ACTIVE_QUESTIONS, "There are no active questions available to start a game")
}
