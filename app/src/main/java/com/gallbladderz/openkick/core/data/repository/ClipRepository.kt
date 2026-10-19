/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.repository


import com.gallbladderz.openkick.core.data.mapper.toUiModel
import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.api.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClipRepository(private val apiService: KickApiService) {
    suspend fun fetchClip(clipId: String): Result<ClipUiModel> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getClip(clipId)
                Result.success(response.clip.toUiModel())
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }
}
