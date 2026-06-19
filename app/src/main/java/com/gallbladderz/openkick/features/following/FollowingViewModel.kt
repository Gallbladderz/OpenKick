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

data class LiveChannelUi(val slug: String, val name: String, val avatarUrl: String)
data class FollowedCategoryUi(val slug: String, val name: String, val bannerUrl: String, val viewers: Int)

sealed interface FollowingUiState {
    data object Loading : FollowingUiState
    data class Success(
        val liveChannels: List<LiveChannelUi>,
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
                    _uiState.value = FollowingUiState.Success(emptyList(), emptyList())
                    return@collect
                }

                val categoriesDeferred = categorySlugs.map { slug ->
                    async { fetchCategoryDetails(slug) }
                }
                val fetchedCategories = categoriesDeferred.awaitAll().filterNotNull()

                val localStreamers = streamerSlugs.map { slug ->
                    LiveChannelUi(
                        slug = slug,
                        name = slug,
                        avatarUrl = "https://ui-avatars.com/api/?name=$slug&background=random"
                    )
                }

                _uiState.value = FollowingUiState.Success(
                    liveChannels = localStreamers,
                    categories = fetchedCategories.sortedByDescending { it.viewers }
                )
            }
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