/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import com.gallbladderz.openkick.core.network.dto.profile.AccountDto
import retrofit2.http.GET

interface AccountApi {
    @GET("api/v1/user")
    suspend fun currentUser(): AccountDto
}
