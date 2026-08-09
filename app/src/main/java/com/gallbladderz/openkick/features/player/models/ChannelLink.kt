/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.models

data class ChannelLink(
    val id: Int,
    val description: String,
    val link: String,
    val title: String,
    val imageUrl: String
)