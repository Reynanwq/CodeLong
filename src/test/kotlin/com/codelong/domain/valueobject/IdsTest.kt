package com.codelong.domain.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class IdsTest {

    @Test
    fun `newUserId gera um UUID valido`() {
        val id = Ids.newUserId()

        assertTrue(id.value.isNotBlank())
        assertEquals(id.value, UUID.fromString(id.value).toString())
    }

    @Test
    fun `newQuestionId gera um UUID valido`() {
        val id = Ids.newQuestionId()

        assertTrue(id.value.isNotBlank())
        assertEquals(id.value, UUID.fromString(id.value).toString())
    }

    @Test
    fun `newGameId gera um UUID valido`() {
        val id = Ids.newGameId()

        assertTrue(id.value.isNotBlank())
        assertEquals(id.value, UUID.fromString(id.value).toString())
    }

    @Test
    fun `ids gerados sao unicos entre chamadas`() {
        val ids = (1..500).map { Ids.newUserId().value }

        assertEquals(500, ids.distinct().size)
    }

    @Test
    fun `UserId e um value object de igualdade por valor`() {
        assertEquals(UserId("abc"), UserId("abc"))
        assertEquals(UserId("abc").hashCode(), UserId("abc").hashCode())
        assertNotEquals(UserId("abc"), UserId("def"))
    }

    @Test
    fun `QuestionId e um value object de igualdade por valor`() {
        assertEquals(QuestionId("q-1"), QuestionId("q-1"))
        assertEquals(QuestionId("q-1").hashCode(), QuestionId("q-1").hashCode())
        assertNotEquals(QuestionId("q-1"), QuestionId("q-2"))
    }

    @Test
    fun `GameId e um value object de igualdade por valor`() {
        assertEquals(GameId("g-1"), GameId("g-1"))
        assertEquals(GameId("g-1").hashCode(), GameId("g-1").hashCode())
        assertNotEquals(GameId("g-1"), GameId("g-2"))
    }

    @Test
    fun `OptionId e um value object de igualdade por valor`() {
        assertEquals(OptionId("a"), OptionId("a"))
        assertEquals(OptionId("a").hashCode(), OptionId("a").hashCode())
        assertNotEquals(OptionId("a"), OptionId("b"))
    }

    @Test
    fun `ids de tipos diferentes com mesmo conteudo nao sao iguais`() {
        assertNotEquals(UserId("x"), QuestionId("x"))
        assertNotEquals(QuestionId("x"), GameId("x"))
        assertNotEquals(GameId("x"), OptionId("x"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["a", "opt-0", "123", "com espaço", "😀", "-"])
    fun `preserva o conteudo bruto recebido`(raw: String) {
        assertEquals(raw, UserId(raw).value)
        assertEquals(raw, QuestionId(raw).value)
        assertEquals(raw, GameId(raw).value)
        assertEquals(raw, OptionId(raw).value)
    }

    @Test
    fun `preserva conteudo longo`() {
        val raw = "x".repeat(300)

        assertEquals(raw, UserId(raw).value)
    }
}
