package com.gallbladderz.openkick.features.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.home.ClipUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface CategoryDetailsUiState {
    data object Loading : CategoryDetailsUiState
    data class Success(
        val name: String,
        val bannerUrl: String,
        val viewers: Int,
        val tags: List<String>,
        val clips: List<ClipUiModel>
    ) : CategoryDetailsUiState
    data class Error(val message: String) : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadCategory(slug: String) {
        _uiState.update { CategoryDetailsUiState.Loading }
        viewModelScope.launch(Dispatchers.IO) {

            Log.d("OpenKick_Category", "Trying to load slug: '$slug'")

            if (slug.isBlank()) {
                _uiState.update { CategoryDetailsUiState.Error("Error: empty slug!") }
                return@launch
            }

            val cleanSlug = slug.trim().lowercase()


            val detailsRequest = Request.Builder()
                .url("https://kick.com/api/v1/subcategories/$cleanSlug")
                .build()

            val clipsRequest = Request.Builder()
                .url("https://kick.com/api/v2/categories/$cleanSlug/clips?sort=view&time=week")
                .build()

            try {

                val detailsDeferred = async { okHttpClient.newCall(detailsRequest).execute() }
                val clipsDeferred = async { okHttpClient.newCall(clipsRequest).execute() }

                val detailsResponse = detailsDeferred.await()
                val clipsResponse = clipsDeferred.await()

                val detailsBody = detailsResponse.body?.string()
                val clipsBody = clipsResponse.body?.string()

                if (!detailsResponse.isSuccessful || detailsBody == null) {
                    _uiState.update { CategoryDetailsUiState.Error("Load error: ${detailsResponse.code}") }
                    return@launch
                }


                parseResponses(detailsBody, clipsBody)

            } catch (e: Exception) {
                _uiState.update { CategoryDetailsUiState.Error("Network error: ${e.message}") }
            }
        }
    }

    private fun parseResponses(detailsJson: String, clipsJson: String?) {
        try {

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(detailsJson).jsonObject

            val name = jsonElement["name"]?.jsonPrimitive?.content ?: "Category"
            val viewers = jsonElement["viewers"]?.jsonPrimitive?.intOrNull ?: 0

            val tagsArray = jsonElement["tags"]?.jsonArray
            val tags = tagsArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            var bannerUrl = jsonElement["banner"]?.jsonObject?.get("srcset")?.jsonPrimitive?.content ?: ""
            bannerUrl = bannerUrl.replace("\\/", "/")
            if (bannerUrl.contains(" ")) {
                bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
            }


            val parsedClips = parseClipsArray(clipsJson)


            _uiState.update { CategoryDetailsUiState.Success(name, bannerUrl, viewers, tags, parsedClips) }
        } catch (e: Exception) {
            _uiState.update { CategoryDetailsUiState.Error("JSON parsing error") }
        }
    }

    private fun parseClipsArray(jsonString: String?): List<ClipUiModel> {
        if (jsonString == null) return emptyList()
        return try {
            val rootObj = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject
            val clipsArray = rootObj["clips"]?.jsonArray ?: emptyList()

            clipsArray.mapNotNull { element ->
                try {
                    val clipObj = element.jsonObject
                    val id = clipObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = clipObj["title"]?.jsonPrimitive?.content ?: "Untitled"
                    val clipUrl = clipObj["clip_url"]?.jsonPrimitive?.content ?: ""
                    val thumbnailUrl = clipObj["thumbnail_url"]?.jsonPrimitive?.content ?: ""
                    val views = clipObj["views"]?.jsonPrimitive?.intOrNull ?: 0
                    val duration = clipObj["duration"]?.jsonPrimitive?.intOrNull ?: 0

                    val minutes = duration / 60
                    val seconds = duration % 60
                    val durationStr = String.format("%02d:%02d", minutes, seconds)

                    ClipUiModel(id, title, thumbnailUrl, clipUrl, views, durationStr)
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e("OpenKick_Category", "Clip parsing error: ${e.message}")
            emptyList()
        }
    }
}