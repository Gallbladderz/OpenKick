/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSocialLinksTest {
    @Test
    fun expandsHandlesAndPlatformPaths() {
        assertEquals("https://youtube.com/@madgostream", normalizeSocialUrl(SocialPlatform.YouTube, "madgostream"))
        assertEquals("https://youtube.com/@name", normalizeSocialUrl(SocialPlatform.YouTube, "  @name "))
        assertEquals("https://youtube.com/channel/UC123", normalizeSocialUrl(SocialPlatform.YouTube, "/channel/UC123"))
        assertEquals("https://instagram.com/name_", normalizeSocialUrl(SocialPlatform.Instagram, "@name_/"))
        assertEquals("https://x.com/name", normalizeSocialUrl(SocialPlatform.X, "@name"))
        assertEquals("https://tiktok.com/@name", normalizeSocialUrl(SocialPlatform.TikTok, "name"))
        assertEquals("https://discord.gg/code", normalizeSocialUrl(SocialPlatform.Discord, "code"))
        assertEquals("https://discord.gg/code", normalizeSocialUrl(SocialPlatform.Discord, "invite/code"))
        assertEquals("https://facebook.com/groups/name", normalizeSocialUrl(SocialPlatform.Facebook, "groups/name"))
        assertEquals("https://facebook.com/profile.php?id=123", normalizeSocialUrl(SocialPlatform.Facebook, "profile.php?id=123"))
    }

    @Test
    fun preservesWebLinksAndRecognizesPlatformDomainsWithoutAProtocol() {
        val original = "https://www.youtube.com/@name?feature=shared"
        assertEquals(original, normalizeSocialUrl(SocialPlatform.YouTube, original))
        assertEquals("https://twitter.com/name", normalizeSocialUrl(SocialPlatform.X, "//twitter.com/name"))
        assertEquals("https://discord.com/invite/code", normalizeSocialUrl(SocialPlatform.Discord, "discord.com/invite/code"))
        assertEquals("https://example.org/about", normalizeSocialUrl(SocialPlatform.Website, "example.org/about"))
    }

    @Test
    fun skipsEmptyValuesAndUnsafeOrMisleadingUrls() {
        assertNull(normalizeSocialUrl(SocialPlatform.YouTube, null))
        assertNull(normalizeSocialUrl(SocialPlatform.YouTube, "   "))
        assertNull(normalizeSocialUrl(SocialPlatform.YouTube, "@"))
        assertNull(normalizeSocialUrl(SocialPlatform.X, "intent://x.com/name"))
        assertNull(normalizeSocialUrl(SocialPlatform.X, "https://x.com@example.org/name"))
        assertNull(normalizeSocialUrl(SocialPlatform.X, "https://x.com.example.org/name"))
        assertNull(normalizeSocialUrl(SocialPlatform.Website, "javascript:alert(0)"))
    }
}
