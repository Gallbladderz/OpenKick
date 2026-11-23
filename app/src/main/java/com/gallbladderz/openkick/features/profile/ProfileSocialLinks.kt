/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import java.net.URI

enum class SocialPlatform(val brandName: String, val host: String, vararg val aliases: String) {
    YouTube("YouTube", "youtube.com", "youtu.be"),
    Instagram("Instagram", "instagram.com", "instagr.am"),
    X("X", "x.com", "twitter.com"),
    TikTok("TikTok", "tiktok.com"),
    Discord("Discord", "discord.gg", "discord.com", "discordapp.com"),
    Facebook("Facebook", "facebook.com", "fb.com", "fb.me"),
    Website("", "")
}

data class ProfileSocialLink(val platform: SocialPlatform, val url: String)

internal fun normalizeSocialUrl(platform: SocialPlatform, raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.startsWith("//") || SCHEME.containsMatchIn(value)) {
        return webUri(if (value.startsWith("//")) "https:$value" else value)
            ?.takeIf { platform == SocialPlatform.Website || platform.ownsHost(it.host) }
            ?.toASCIIString()
    }

    val firstSegment = value.trimStart('/').substringBefore('/').substringBefore('?').substringBefore('#')
    if (platform == SocialPlatform.Website || platform.ownsHost(firstSegment)) {
        val url = webUri("https://${value.trimStart('/')}") ?: return null
        return url.toASCIIString().takeIf { platform != SocialPlatform.Website || '.' in url.host }
    }

    val relative = value.trimStart('/')
    val pathWithSlash = relative.substringBefore('?').substringBefore('#')
    val suffix = relative.drop(pathWithSlash.length)
    val path = pathWithSlash.trimEnd('/')
    if (!SOCIAL_PATH.matches(path) || path.split('/').any { it == "." || it == ".." }) return null
    val isHandle = '/' !in path
    val normalizedPath = when (platform) {
        SocialPlatform.YouTube, SocialPlatform.TikTok -> if (isHandle) "@${path.trimStart('@')}" else path
        SocialPlatform.Discord -> path.removePrefix("invite/").trimStart('@')
        else -> path.trimStart('@')
    }
    if (normalizedPath.isBlank() || normalizedPath == "@") return null
    return webUri("https://${platform.host}/$normalizedPath$suffix")?.toASCIIString()
}

internal fun webUri(value: String): URI? =
    runCatching { URI(value) }.getOrNull()?.takeIf {
        (it.scheme.equals("https", ignoreCase = true) || it.scheme.equals("http", ignoreCase = true)) &&
            it.host != null && it.rawUserInfo == null
    }

private fun SocialPlatform.ownsHost(candidate: String): Boolean =
    sequenceOf(host, *aliases).any { domain ->
        candidate.equals(domain, ignoreCase = true) || candidate.endsWith(".$domain", ignoreCase = true)
    }

private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val SOCIAL_PATH = Regex("@?[\\p{L}\\p{N}._-]+(?:/[@\\p{L}\\p{N}._-]+)*")
