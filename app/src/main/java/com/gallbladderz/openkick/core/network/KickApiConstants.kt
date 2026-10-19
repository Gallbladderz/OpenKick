/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network

object KickApiConstants {
    const val APP_VERSION = "40.32.1"
    const val APP_BUILD = "60006965"
    const val USER_AGENT =
        "KickMobile/$APP_VERSION (com.kick.mobile; platform: android; build:$APP_BUILD)"

    const val CENTRIFUGO_WS_URL = "wss://realtime.us-east-1.platform.kick.com/connection/websocket"
    const val REALTIME_AUTH_URL = "https://mobile.kick.com/api/v1/realtime/auth/connection"

    const val KICK_BASE_URL = "https://kick.com"
    const val KICK_MOBILE_API_BASE_URL = "https://mobile.kick.com/api/v1"
    const val KICK_API_BASE_URL = "https://kick.com/api/v1"
    const val KICK_API_V2_BASE_URL = "https://kick.com/api/v2"
    const val KICK_SEARCH_API_BASE_URL = "https://search.kick.com/api/v1"

    const val HEADER_PLATFORM = "Android"
    const val HEADER_APP = "mobile"
}
