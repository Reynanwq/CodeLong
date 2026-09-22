package com.codelong.infrastructure.web.controller

import com.codelong.application.usecase.ListThemeQuestionsUseCase
import com.codelong.application.usecase.ListThemesUseCase
import com.codelong.domain.valueobject.Category
import com.codelong.infrastructure.web.dto.LearningQuestionResponse
import com.codelong.infrastructure.web.dto.ThemeQuestionsResponse
import com.codelong.infrastructure.web.dto.ThemeResponse
import com.codelong.infrastructure.web.dto.ThemesResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val BASE_PATH = "/api/learning"
private const val THEMES_PATH = "/themes"
private const val THEME_QUESTIONS_PATH = "/themes/{category}/questions"

@RestController
@RequestMapping(BASE_PATH)
class LearningController(
    private val listThemesUseCase: ListThemesUseCase,
    private val listThemeQuestionsUseCase: ListThemeQuestionsUseCase
) {

    @GetMapping(THEMES_PATH)
    fun themes(): ThemesResponse =
        ThemesResponse(listThemesUseCase.themes().map(ThemeResponse::from))

    @GetMapping(THEME_QUESTIONS_PATH)
    fun questions(@PathVariable category: String): ThemeQuestionsResponse =
        ThemeQuestionsResponse(
            listThemeQuestionsUseCase.questions(Category.fromName(category)).map(LearningQuestionResponse::from)
        )
}
