/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import com.gallbladderz.openkick.core.network.chat.SevenTvConnectionDto
import com.gallbladderz.openkick.core.network.chat.SevenTvEmoteSetDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

internal interface SevenTvApi {
    @GET("emote-sets/global")
    suspend fun global(): Response<SevenTvEmoteSetDto>

    @GET("users/kick/{id}")
    suspend fun channel(@Path("id") kickUserId: Long): Response<SevenTvConnectionDto>
}
