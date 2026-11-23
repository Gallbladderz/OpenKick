/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KickWebLinkTest {
    @Test
    fun routesLinksToExistingScreens() {
        assertEquals(
            ClipPlayerRoute("clip_01M7QBJDMN1VY7V8T20BK39CAC"),
            parseKickWebLink("https://kick.com/clips/clip_01M7QBJDMN1VY7V8T20BK39CAC?ref=share")?.destination
        )
        assertEquals(
            StreamerProfileRoute("ozon689games3"),
            parseKickWebLink("http://www.kick.com/Ozon689Games3/")?.destination
        )
        assertEquals(
            CategoryDetailsRoute("just-chatting"),
            parseKickWebLink("https://kick.com/categories/just-chatting")?.destination
        )
    }

    @Test
    fun acceptsVodLinksWithOrWithoutChannel() {
        val id = "c8745a96-3723-4d19-8d08-2514c4453fe0"
        assertEquals(VodPlayerRoute(id), parseKickWebLink("https://kick.com/video/$id")?.destination)
        for (path in listOf("video/ozon689games3/$id", "ozon689games3/videos/$id")) {
            assertEquals(
                VodPlayerRoute(id, "ozon689games3"),
                parseKickWebLink("https://kick.com/$path")?.destination
            )
        }
    }

    @Test
    fun ignoresOtherSitesAndLeavesUnsupportedKickPagesForTheBrowser() {
        assertNull(parseKickWebLink("https://kick.com.example.org/ozon689games3"))
        assertNull(parseKickWebLink("https://example.org@kick.com/ozon689games3"))
        assertNull(parseKickWebLink("openkick://oauth/callback?code=sample"))

        val unsupported = parseKickWebLink("https://kick.com/privacy-policy")
        assertNotNull(unsupported)
        assertNull(unsupported?.destination)
        assertNull(parseKickWebLink("https://kick.com/clips%2Fclip_example")?.destination)
        assertNull(parseKickWebLink("https://kick.com/video/explicitly-invalid")?.destination)
    }
}
