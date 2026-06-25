package com.gallbladderz.openkick.features.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


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
    private val followingRepository: FollowingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun unfollowStreamer(slug: String) {
        viewModelScope.launch {


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
                    _uiState.update { FollowingUiState.Success(emptyList(), emptyList(), emptyList()) }
                    return@collect
                }


                val categoriesDeferred = categorySlugs.map { slug ->
                    async { followingRepository.fetchCategoryDetails(slug) }
                }

                val streamersDeferred = streamerSlugs.map { slug ->
                    async { followingRepository.fetchChannelDetails(slug) }
                }

                val fetchedCategories = categoriesDeferred.awaitAll().filterNotNull()
                val fetchedStreamers = streamersDeferred.awaitAll().filterNotNull()

                val liveStreamers = fetchedStreamers.filter { it.isLive }.sortedByDescending { it.viewers }
                val offlineStreamers = fetchedStreamers.filter { !it.isLive }.sortedBy { it.username.lowercase() }

                _uiState.update {
                    FollowingUiState.Success(
                        liveStreamers = liveStreamers,
                        offlineStreamers = offlineStreamers,
                        categories = fetchedCategories.sortedByDescending { it.viewers }
                    )
                }
            }
        }
    }
}