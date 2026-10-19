/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.dto.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RealtimeConnectionRequest(
    @SerialName("client_id") val clientId: String
)

@Serializable
data class RealtimeConnectionResponse(
    @SerialName("data") val data: RealtimeTokenData,
    @SerialName("message") val message: String
)

@Serializable
data class RealtimeTokenData(
    @SerialName("token") val token: String
)

