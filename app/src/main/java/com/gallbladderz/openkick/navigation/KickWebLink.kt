/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.navigation

import java.net.URI
import java.util.Locale

internal data class KickWebLink(val url: String, val destination: KickLinkDestination?)

internal fun parseKickWebLink(url: String): KickWebLink? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val defaultPort = when (uri.scheme?.lowercase(Locale.ROOT)) {
        "https" -> 443
        "http" -> 80
        else -> return null
    }
    if (uri.host?.lowercase(Locale.ROOT) !in kickHosts || uri.rawUserInfo != null ||
        (uri.port != -1 && uri.port != defaultPort)
    ) return null

    val path = uri.path.orEmpty().removePrefix("/").removeSuffix("/")
    val segments = path.split('/')
    val destination = if (uri.rawPath.orEmpty().contains("%2f", ignoreCase = true) ||
        segments.any { !slugPattern.matches(it) }
    ) {
        null
    } else {
        val first = segments.first().lowercase(Locale.ROOT)
        when (segments.size) {
            1 -> if (first !in websitePages) StreamerProfileRoute(first) else null
            2 -> when (first) {
                "clips" -> segments[1].toClipRoute()
                "categories" -> CategoryDetailsRoute(segments[1].lowercase(Locale.ROOT))
                "video" -> segments[1].toVodRoute()
                else -> null
            }
            3 -> when {
                first == "video" -> segments[2].toVodRoute(segments[1].lowercase(Locale.ROOT))
                first !in websitePages && segments[1] == "videos" -> segments[2].toVodRoute(first)
                first !in websitePages && segments[1] == "clips" -> segments[2].toClipRoute()
                else -> null
            }
            else -> null
        }
    }
    return KickWebLink(url, destination)
}

private fun String.toClipRoute(): ClipPlayerRoute? =
    takeIf(clipIdPattern::matches)?.let(::ClipPlayerRoute)

private fun String.toVodRoute(channelSlug: String? = null): VodPlayerRoute? =
    takeIf(videoIdPattern::matches)?.let { VodPlayerRoute(it, channelSlug) }

private val kickHosts = setOf("kick.com", "www.kick.com")
private val slugPattern = Regex("[A-Za-z0-9_-]{1,128}")
private val clipIdPattern = Regex("clip_[A-Za-z0-9]+")
private val videoIdPattern = Regex("[0-9]+|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
private val websitePages = setOf(
    "about", "api", "browse", "categories", "clips", "community-guidelines", "dashboard",
    "downloads", "following", "help", "login", "privacy-policy", "safety", "search",
    "settings", "signup", "terms-of-service", "video", "videos"
)
