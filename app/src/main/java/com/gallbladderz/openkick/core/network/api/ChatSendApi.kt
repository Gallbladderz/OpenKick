/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import androidx.annotation.Keep
import com.gallbladderz.openkick.core.network.chat.SendChatMessageRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

@Keep
interface ChatSendApi {
    @POST("api/v2/messages/send/{chatroomId}")
    suspend fun sendChatMessage(
        @Path("chatroomId") chatroomId: String,
        @Body request: SendChatMessageRequest
    ): ResponseBody?
}
