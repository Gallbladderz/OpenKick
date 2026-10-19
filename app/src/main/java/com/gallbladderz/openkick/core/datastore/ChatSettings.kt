/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.datastore

data class ChatSettings(
    val textScale: Float = 1f,
    val showGlobalBadges: Boolean = true,
    val showChannelBadges: Boolean = true,
    val useOfficialNameColor: Boolean = true,
    val animatedEmotes: Boolean = true
)
