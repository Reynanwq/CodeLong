package com.codelong.domain.valueobject

import com.codelong.domain.exception.DomainException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class EnumsTest {

    @ParameterizedTest
    @EnumSource(Category::class)
    fun `Category fromName aceita o proprio nome`(category: Category) {
        assertEquals(category, Category.fromName(category.name))
    }

    @ParameterizedTest
    @EnumSource(Category::class)
    fun `Category fromName aceita nome em minusculo`(category: Category) {
        assertEquals(category, Category.fromName(category.name.lowercase()))
    }

    @ParameterizedTest
    @EnumSource(Category::class)
    fun `Category fromName aceita nome em caixa mista`(category: Category) {
        assertEquals(category, Category.fromName(category.name.lowercase().replaceFirstChar { it.uppercase() }))
    }

    @ParameterizedTest
    @ValueSource(strings = ["JAVASCRIPT", "kotlin ", " kotlin", "UNKNOWN", "", " "])
    fun `Category fromName rejeita valores desconhecidos`(raw: String) {
        val error = assertThrows<DomainException> { Category.fromName(raw) }

        assertEquals("category.invalid", error.code)
        assertTrue(error.message.contains(raw))
    }

    @Test
    fun `Category possui as 24 categorias esperadas`() {
        assertEquals(24, Category.entries.size)
    }

    @ParameterizedTest
    @EnumSource(Role::class)
    fun `Role fromName aceita o proprio nome e minusculo`(role: Role) {
        assertEquals(role, Role.fromName(role.name))
        assertEquals(role, Role.fromName(role.name.lowercase()))
    }

    @ParameterizedTest
    @ValueSource(strings = ["GUEST", " admin ", "SUPER_ADMIN", "", "user "])
    fun `Role fromName rejeita valores desconhecidos`(raw: String) {
        val error = assertThrows<DomainException> { Role.fromName(raw) }

        assertEquals("role.invalid", error.code)
        assertTrue(error.message.contains(raw))
    }

    @ParameterizedTest
    @EnumSource(AccountStatus::class)
    fun `AccountStatus fromName aceita o proprio nome`(status: AccountStatus) {
        assertEquals(status, AccountStatus.fromName(status.name))
        assertEquals(status, AccountStatus.fromName(status.name.lowercase()))
        assertEquals(status, AccountStatus.fromName("  ${status.name.lowercase()}  "))
    }

    @ParameterizedTest
    @ValueSource(strings = ["PENDING", "BANNED", "", " ", "active_", "inactive!"])
    fun `AccountStatus fromName rejeita valores desconhecidos`(raw: String) {
        val error = assertThrows<DomainException> { AccountStatus.fromName(raw) }

        assertEquals("status.invalid", error.code)
    }

    @ParameterizedTest
    @EnumSource(GameStatus::class)
    fun `GameStatus fromName aceita o proprio nome com espacos`(status: GameStatus) {
        assertEquals(status, GameStatus.fromName(status.name))
        assertEquals(status, GameStatus.fromName(status.name.lowercase()))
        assertEquals(status, GameStatus.fromName(" ${status.name.lowercase()} "))
    }

    @ParameterizedTest
    @ValueSource(strings = ["FINISHED", "PAUSED", "", " ", "in_progress_", "completed!"])
    fun `GameStatus fromName rejeita valores desconhecidos`(raw: String) {
        val error = assertThrows<DomainException> { GameStatus.fromName(raw) }

        assertEquals("status.invalid", error.code)
        assertTrue(error.message.contains(raw))
    }

    @Test
    fun `GameStatus possui os tres estados esperados`() {
        assertEquals(listOf(GameStatus.IN_PROGRESS, GameStatus.COMPLETED, GameStatus.ABANDONED), GameStatus.entries)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
    fun `Difficulty fromLevel aceita niveis de 1 a 10`(level: Int) {
        val difficulty = Difficulty.fromLevel(level)

        assertEquals(level, difficulty.level)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, -10, 11, 12, 100, Int.MIN_VALUE, Int.MAX_VALUE])
    fun `Difficulty fromLevel rejeita niveis fora do intervalo`(level: Int) {
        val error = assertThrows<DomainException> { Difficulty.fromLevel(level) }

        assertEquals("difficulty.invalid", error.code)
        assertEquals("Difficulty level must be between 1 and 10", error.message)
    }

    @Test
    fun `Difficulty pontos equivalem ao nivel vezes dez`() {
        Difficulty.entries.forEach { difficulty ->
            assertEquals(difficulty.level * 10, difficulty.points)
        }
    }

    @Test
    fun `Difficulty possui dez niveis`() {
        assertEquals(10, Difficulty.entries.size)
    }

    @Test
    fun `Difficulty orderedByLevel esta ordenado crescentemente`() {
        val ordered = Difficulty.orderedByLevel

        assertEquals(Difficulty.entries.sortedBy { it.level }, ordered)
        assertEquals(ordered.sortedBy { it.level }, ordered)
        assertEquals(Difficulty.VERY_EASY, ordered.first())
        assertEquals(Difficulty.MASTER, ordered.last())
    }

    @Test
    fun `Difficulty e comparavel por ordem de declaracao`() {
        assertTrue(Difficulty.VERY_EASY < Difficulty.EASY)
        assertTrue(Difficulty.EASY < Difficulty.MASTER)
        assertTrue(Difficulty.MASTER > Difficulty.HARD)
    }

    @Test
    fun `QuestionStatus possui os dois estados esperados`() {
        assertEquals(listOf(QuestionStatus.ACTIVE, QuestionStatus.INACTIVE), QuestionStatus.entries)
    }

    @ParameterizedTest
    @EnumSource(GameMode::class)
    fun `GameMode fromName aceita o proprio nome`(mode: GameMode) {
        assertEquals(mode, GameMode.fromName(mode.name))
        assertEquals(mode, GameMode.fromName(mode.name.lowercase()))
        assertEquals(mode, GameMode.fromName("  ${mode.name.lowercase()}  "))
    }

    @ParameterizedTest
    @ValueSource(strings = ["INVALIDO", "", " ", "genocida_", "classic!"])
    fun `GameMode fromName rejeita valores desconhecidos`(raw: String) {
        val error = assertThrows<DomainException> { GameMode.fromName(raw) }

        assertEquals("game.mode.invalid", error.code)
        assertTrue(error.message.contains(raw))
    }

    @Test
    fun `GameMode possui os dois modos esperados`() {
        assertEquals(listOf(GameMode.CLASSIC, GameMode.GENOCIDA), GameMode.entries)
    }
}