/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.core.network.dto.realtime.RealtimeConnectionRequest
import com.gallbladderz.openkick.core.network.dto.realtime.RealtimeConnectionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface RealtimeApi {
    @POST(KickApiConstants.REALTIME_AUTH_URL)
    suspend fun getConnectionToken(
        @Body request: RealtimeConnectionRequest
    ): RealtimeConnectionResponse
}
