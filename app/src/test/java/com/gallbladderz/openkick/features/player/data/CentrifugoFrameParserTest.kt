/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.network.chat.CentrifugoFrameParser

import com.gallbladderz.openkick.core.network.chat.CentrifugoErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentrifugoFrameParserTest {

    @Test
    fun `extracts plain json object`() {
        val json = """{"a":1}"""

        val result = CentrifugoFrameParser.extractJsonObject(json, 0)

        assertEquals(json, result)
    }

    @Test
    fun `strips protobuf tail after json object`() {

        val raw = """{"event":"x"}:${'$'}unknown"""

        val result = CentrifugoFrameParser.extractJsonObject(raw, 0)

        assertEquals("""{"event":"x"}""", result)
    }

    @Test
    fun `closing brace inside string value does not truncate object`() {
        val raw = """{"content":"месседж } тут конец"}"""

        val result = CentrifugoFrameParser.extractJsonObject(raw, 0)

        assertEquals(raw, result)
    }

    @Test
    fun `escaped quote does not toggle string state`() {
        val raw = """{"content":"он сказал \"привет\" и ушёл"}"""

        val result = CentrifugoFrameParser.extractJsonObject(raw, 0)

        assertEquals(raw, result)
    }

    @Test
    fun `handles nested objects`() {
        val raw = """{"outer":{"inner":{"deep":1}}}"""

        val result = CentrifugoFrameParser.extractJsonObject(raw, 0)

        assertEquals(raw, result)
    }

    @Test
    fun `returns null when braces never balance`() {
        assertNull(CentrifugoFrameParser.extractJsonObject("""{"a":1""", 0))
    }

    @Test
    fun `returns null when start index is out of bounds or not a brace`() {
        assertNull(CentrifugoFrameParser.extractJsonObject("abc", 0))
        assertNull(CentrifugoFrameParser.extractJsonObject("""{"a":1}""", 99))
        assertNull(CentrifugoFrameParser.extractJsonObject("""{"a":1}""", -1))
    }

    @Test
    fun `parses push with pub payload`() {
        val raw = """{"push":{"channel":"chatrooms.123.v2",""" +
            """"pub":{"event":"App\\Events\\ChatMessageEvent","data":"{\"id\":\"1\"}"}}}"""

        val frame = CentrifugoFrameParser.parse(raw)

        assertNotNull(frame)
        val push = frame!!.push!!
        assertEquals("chatrooms.123.v2", push.channel)
        assertEquals("App\\Events\\ChatMessageEvent", push.pub!!.event)
    }

    @Test
    fun `parses push where payload is in data field`() {
        val raw = """{"push":{"channel":"c","data":{"event":"E","data":"x"}}}"""

        val frame = CentrifugoFrameParser.parse(raw)

        assertNotNull(frame)
        assertEquals("E", frame!!.push!!.data!!.event)
    }

    @Test
    fun `parses event at top level without push wrapper (Kick format)`() {
        val raw = """{"channel":"chatrooms.123.v2","event":"App\\Events\\ChatMessageEvent","data":"{\"id\":\"1\"}"}"""

        val frame = CentrifugoFrameParser.parse(raw)

        assertNotNull(frame)
        assertEquals("App\\Events\\ChatMessageEvent", frame!!.event)
        assertEquals("chatrooms.123.v2", frame.channel)
        assertNotNull(frame.resolvedEvent)
        assertEquals("App\\Events\\ChatMessageEvent", frame.resolvedEvent!!.event)
    }

    @Test
    fun `parses error reply`() {
        val raw = """{"id":1,"error":{"code":109,"message":"unauthorized","temporary":false}}"""

        val frame = CentrifugoFrameParser.parse(raw)

        assertNotNull(frame)
        assertEquals(1, frame!!.id)
        assertEquals(109, frame.error!!.code)
        assertEquals("unauthorized", frame.error!!.message)
        assertEquals(false, frame.error!!.temporary)
    }

    @Test
    fun `parses success reply without error`() {
        val frame = CentrifugoFrameParser.parse("""{"id":1}""")

        assertNotNull(frame)
        assertEquals(1, frame!!.id)
        assertNull(frame.error)
    }

    @Test
    fun `returns null for ping frame without json`() {
        assertNull(CentrifugoFrameParser.parse("\u0000"))
    }

    @Test
    fun `returns null for empty frame`() {
        assertNull(CentrifugoFrameParser.parse(""))
    }

    @Test
    fun `describe maps known codes`() {
        assertTrue(
            CentrifugoErrors.describe(CentrifugoErrors.UNAUTHORIZED, null).contains("Токен")
        )
        assertTrue(
            CentrifugoErrors.describe(CentrifugoErrors.PERMISSION_DENIED, null).contains("доступа")
        )
    }

    @Test
    fun `describe falls back to server message for unknown code`() {
        val result = CentrifugoErrors.describe(9999, "что-то неожиданное")

        assertEquals("что-то неожиданное", result)
    }

    @Test
    fun `describe uses generic text when code and message are unknown`() {
        val result = CentrifugoErrors.describe(9999, null)

        assertEquals("Ошибка сервера чата", result)
    }

    @Test
    fun `describe ignores blank server message`() {
        val result = CentrifugoErrors.describe(null, "   ")

        assertEquals("Ошибка сервера чата", result)
    }
}
