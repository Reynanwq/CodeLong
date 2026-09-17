package com.codelong.infrastructure.web.controller

import com.codelong.application.command.ChangeQuestionStatusCommand
import com.codelong.application.command.QuestionSearchQuery
import com.codelong.application.usecase.ChangeQuestionStatusUseCase
import com.codelong.application.usecase.CreateQuestionUseCase
import com.codelong.application.usecase.DeleteQuestionUseCase
import com.codelong.application.usecase.GetQuestionUseCase
import com.codelong.application.usecase.ListQuestionsUseCase
import com.codelong.application.usecase.UpdateQuestionUseCase
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Difficulty
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.infrastructure.web.dto.ChangeQuestionStatusRequest
import com.codelong.infrastructure.web.dto.CreateQuestionRequest
import com.codelong.infrastructure.web.dto.PageResponse
import com.codelong.infrastructure.web.dto.QuestionResponse
import com.codelong.infrastructure.web.dto.UpdateQuestionRequest
import com.codelong.infrastructure.web.dto.toCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/admin/questions"
private const val DEFAULT_PAGE = "0"
private const val DEFAULT_SIZE = "20"
private const val QUESTION_PATH = "/{questionId}"
private const val STATUS_PATH = "/{questionId}/status"

@RestController
@RequestMapping(BASE_PATH)
class AdminQuestionController(
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val updateQuestionUseCase: UpdateQuestionUseCase,
    private val changeQuestionStatusUseCase: ChangeQuestionStatusUseCase,
    private val getQuestionUseCase: GetQuestionUseCase,
    private val listQuestionsUseCase: ListQuestionsUseCase,
    private val deleteQuestionUseCase: DeleteQuestionUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateQuestionRequest): QuestionResponse =
        QuestionResponse.from(createQuestionUseCase.create(request.toCommand()))

    @GetMapping(QUESTION_PATH)
    fun get(@PathVariable questionId: String): QuestionResponse =
        QuestionResponse.from(getQuestionUseCase.get(QuestionId(questionId)))

    @GetMapping
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) difficulty: String?,
        @RequestParam(defaultValue = DEFAULT_PAGE) page: Int,
        @RequestParam(defaultValue = DEFAULT_SIZE) size: Int
    ): PageResponse<QuestionResponse> {
        val query = QuestionSearchQuery(
            status = status?.let { QuestionStatus.valueOf(it.trim().uppercase()) },
            category = category?.let { Category.fromName(it) },
            difficulty = difficulty?.let { Difficulty.valueOf(it.trim().uppercase()) },
            page = page,
            size = size
        )
        val result = listQuestionsUseCase.list(query)
        return PageResponse.of(
            items = result.items.map(QuestionResponse::from),
            totalElements = result.totalElements,
            page = result.page,
            size = result.size
        )
    }

    @PutMapping(QUESTION_PATH)
    fun update(
        @PathVariable questionId: String,
        @Valid @RequestBody request: UpdateQuestionRequest
    ): QuestionResponse =
        QuestionResponse.from(updateQuestionUseCase.update(request.toCommand(QuestionId(questionId))))

    @PatchMapping(STATUS_PATH)
    fun changeStatus(
        @PathVariable questionId: String,
        @Valid @RequestBody request: ChangeQuestionStatusRequest
    ): QuestionResponse =
        QuestionResponse.from(
            changeQuestionStatusUseCase.change(
                ChangeQuestionStatusCommand(QuestionId(questionId), request.active)
            )
        )

    @DeleteMapping(QUESTION_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable questionId: String) {
        deleteQuestionUseCase.delete(QuestionId(questionId))
    }
}