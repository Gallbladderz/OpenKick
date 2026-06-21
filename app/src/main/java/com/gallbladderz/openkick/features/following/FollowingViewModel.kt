package com.gallbladderz.openkick.features.following

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request


data class FollowedStreamerUi(
    val slug: String,
    val username: String,
    val avatarUrl: String,
    val isLive: Boolean,
    val streamTitle: String = "",
    val viewers: Int = 0,
    val categoryName: String = "",
    val streamThumbnailUrl: String = ""
)

data class FollowedCategoryUi(val slug: String, val name: String, val bannerUrl: String, val viewers: Int)

sealed interface FollowingUiState {
    data object Loading : FollowingUiState
    data class Success(
        val liveStreamers: List<FollowedStreamerUi>,
        val offlineStreamers: List<FollowedStreamerUi>, 
        val categories: List<FollowedCategoryUi>
    ) : FollowingUiState
    data class Error(val message: String) : FollowingUiState
}

class FollowingViewModel(
    private val followsRepository: FollowsRepository,
    private val okHttpClient: OkHttpClient
) : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun unfollowStreamer(slug: String) {
        viewModelScope.launch(Dispatchers.IO) {
            
            followsRepository.toggleStreamerFollow(slug, true)
        }
    }

    init {
        observeFollows()
    }

    private fun observeFollows() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                followsRepository.getFollowedCategoriesSlugs(),
                followsRepository.getFollowedStreamersSlugs()
            ) { categorySlugs, streamerSlugs ->
                Pair(categorySlugs, streamerSlugs)
            }.collect { (categorySlugs, streamerSlugs) ->

                if (categorySlugs.isEmpty() && streamerSlugs.isEmpty()) {
                    _uiState.value = FollowingUiState.Success(emptyList(), emptyList(), emptyList())
                    return@collect
                }

                
                val categoriesDeferred = categorySlugs.map { slug ->
                    async { fetchCategoryDetails(slug) }
                }

                
                val streamersDeferred = streamerSlugs.map { slug ->
                    async { fetchChannelDetails(slug) }
                }

                val fetchedCategories = categoriesDeferred.awaitAll().filterNotNull()
                val fetchedStreamers = streamersDeferred.awaitAll().filterNotNull()

                
                val liveStreamers = fetchedStreamers.filter { it.isLive }.sortedByDescending { it.viewers }
                val offlineStreamers = fetchedStreamers.filter { !it.isLive }.sortedBy { it.username.lowercase() }

                _uiState.value = FollowingUiState.Success(
                    liveStreamers = liveStreamers,
                    offlineStreamers = offlineStreamers,
                    categories = fetchedCategories.sortedByDescending { it.viewers }
                )
            }
        }
    }

    private fun fetchChannelDetails(slug: String): FollowedStreamerUi? {
        val request = Request.Builder()
            .url("https://kick.com/api/v1/channels/$slug")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null

            val root = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val userObj = root["user"]?.jsonObject

            val username = userObj?.get("username")?.jsonPrimitive?.content ?: slug
            val avatarUrl = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""

            
            val livestreamObj = root["livestream"]?.jsonObject
            if (livestreamObj != null) {
                val title = livestreamObj["session_title"]?.jsonPrimitive?.content ?: "Без названия"
                val viewers = livestreamObj["viewer_count"]?.jsonPrimitive?.intOrNull ?: 0
                val categoryName = livestreamObj["category"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""

                
                val thumbObj = livestreamObj["thumbnail"]?.jsonObject
                val streamThumbnailUrl = thumbObj?.get("url")?.jsonPrimitive?.content
                    ?: thumbObj?.get("src")?.jsonPrimitive?.content ?: ""

                FollowedStreamerUi(slug, username, avatarUrl, true, title, viewers, categoryName, streamThumbnailUrl)
            } else {
                
                FollowedStreamerUi(slug, username, avatarUrl, false)
            }
        } catch (e: Exception) {
            Log.e("FollowingVM", "Ошибка загрузки канала $slug: ${e.message}")
            null
        }
    }

    private fun fetchCategoryDetails(slug: String): FollowedCategoryUi? {
        val request = Request.Builder()
            .url("https://kick.com/api/v1/subcategories/$slug")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val name = jsonElement["name"]?.jsonPrimitive?.content ?: slug
            val viewers = jsonElement["viewers"]?.jsonPrimitive?.intOrNull ?: 0

            var bannerUrl = jsonElement["banner"]?.jsonObject?.get("srcset")?.jsonPrimitive?.content ?: ""
            bannerUrl = bannerUrl.replace("\\/", "/")
            if (bannerUrl.contains(" ")) {
                bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
            }

            FollowedCategoryUi(slug, name, bannerUrl, viewers)
        } catch (e: Exception) {
            Log.e("FollowingVM", "Ошибка загрузки категории $slug: ${e.message}")
            null
        }
    }
}