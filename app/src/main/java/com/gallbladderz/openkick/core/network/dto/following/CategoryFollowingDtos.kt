/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.dto.following

import kotlinx.serialization.Serializable

@Serializable
data class RemoteFollowedCategory(
    val slug: String = "",
    val name: String = ""
)

