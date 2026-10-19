/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path


interface ChatEmotesApi {
    @GET("emotes/{channel}")
    suspend fun getChannelEmotes(@Path("channel") channel: String): JsonElement

    @GET("api/v2/channels/{channel}/emotes")
    suspend fun getChannelEmotesV2(@Path("channel") channel: String): JsonElement
}
