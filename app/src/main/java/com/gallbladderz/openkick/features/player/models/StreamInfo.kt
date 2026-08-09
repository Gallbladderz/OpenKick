/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.models

data class StreamInfo(
    val playbackUrl: String,
    val avatarUrl: String,
    val viewers: Int,
    val title: String,
    val chatroomId: String?,
    val categoryName: String?,
    val categorySlug: String?
)
