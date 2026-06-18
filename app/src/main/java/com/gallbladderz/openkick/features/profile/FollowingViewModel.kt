package com.gallbladderz.openkick.features.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class FollowedStreamer(
    val slug: String,
    val avatarUrl: String,
    val isLive: Boolean
)

class FollowingViewModel(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<FollowedStreamer>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.followedChannelsFlow.collect { slugs ->
                fetchFollowedData(slugs)
            }
        }
    }

    private suspend fun fetchFollowedData(slugs: Set<String>) {
        if (slugs.isEmpty()) {
            _uiState.value = emptyList()
            _isLoading.value = false
            return
        }

        _isLoading.value = true

        val results = slugs.map { slug ->
            viewModelScope.async(Dispatchers.IO) {
                fetchSingleChannel(slug)
            }
        }.awaitAll().filterNotNull()

        _uiState.value = results.sortedByDescending { it.isLive }
        _isLoading.value = false
    }

    private fun fetchSingleChannel(slug: String): FollowedStreamer? {
        val request = Request.Builder()
            .url("https://kick.com/api/v2/channels/$slug")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val userObj = json["user"]?.jsonObject

            var avatar = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""
            avatar = avatar.replace("\\/", "/")

            val playbackUrl = json["playback_url"]?.jsonPrimitive?.content
            val isLive = !playbackUrl.isNullOrEmpty()

            FollowedStreamer(slug, avatar, isLive)
        } catch (e: Exception) {
            Log.e("OpenKick_Following", "Ошибка загрузки $slug: ${e.message}")
            null
        }
    }

    fun unfollow(slug: String) {
        viewModelScope.launch { settingsRepository.toggleFollow(slug) }
    }
}