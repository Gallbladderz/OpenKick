/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowType
import com.gallbladderz.openkick.data.local.FollowedEntity
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

data class FollowedCategoryUi(
    val slug: String,
    val name: String,
    val bannerUrl: String,
    val viewers: Int
)

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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        observeFollows()
        viewModelScope.launch {
            refreshTrigger.collect {
                syncWithApi()
            }
        }
        syncWithApi()
    }

    fun unfollowStreamer(slug: String) {
        viewModelScope.launch {
            followsRepository.toggleStreamerFollow(slug, true)
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        refreshTrigger.tryEmit(Unit)
    }

    private fun observeFollows() {
        viewModelScope.launch(Dispatchers.IO) {
            followsRepository.getAllFollowedEntities().collectLatest { entities ->
                val categories = entities.filter { it.type == FollowType.CATEGORY }.map {
                    FollowedCategoryUi(
                        slug = it.slug,
                        name = it.categoryName,
                        bannerUrl = it.bannerUrl,
                        viewers = it.viewers
                    )
                }.sortedByDescending { it.viewers }

                val streamers = entities.filter { it.type == FollowType.STREAMER }.map {
                    FollowedStreamerUi(
                        slug = it.slug,
                        username = it.username,
                        avatarUrl = it.avatarUrl,
                        isLive = it.isLive,
                        streamTitle = it.streamTitle,
                        viewers = it.viewers,
                        categoryName = it.categoryName,
                        streamThumbnailUrl = it.bannerUrl
                    )
                }

                val liveStreamers = streamers.filter { it.isLive }.sortedByDescending { it.viewers }
                val offlineStreamers =
                    streamers.filter { !it.isLive }.sortedBy { it.username.lowercase() }

                _uiState.update {
                    FollowingUiState.Success(
                        liveStreamers = liveStreamers,
                        offlineStreamers = offlineStreamers,
                        categories = categories
                    )
                }
            }
        }
    }

    private fun syncWithApi() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.update { true }
            val entities = followsRepository.getAllFollowedEntities().first()
            val streamerSlugs = entities.filter { it.type == FollowType.STREAMER }.map { it.slug }
            val categorySlugs = entities.filter { it.type == FollowType.CATEGORY }.map { it.slug }

            val streamerJobs = streamerSlugs.map { slug ->
                async {
                    followingRepository.fetchChannelDetails(slug).getOrNull()?.let { ui ->
                        followsRepository.saveFollowedEntity(
                            FollowedEntity(
                                slug = ui.slug,
                                type = FollowType.STREAMER,
                                isLive = ui.isLive,
                                username = ui.username,
                                avatarUrl = ui.avatarUrl,
                                streamTitle = ui.streamTitle,
                                viewers = ui.viewers,
                                categoryName = ui.categoryName,
                                bannerUrl = ui.streamThumbnailUrl
                            )
                        )
                    }
                }
            }

            val categoryJobs = categorySlugs.map { slug ->
                async {
                    followingRepository.fetchCategoryDetails(slug).getOrNull()?.let { ui ->
                        followsRepository.saveFollowedEntity(
                            FollowedEntity(
                                slug = ui.slug,
                                type = FollowType.CATEGORY,
                                isLive = false,
                                username = "",
                                avatarUrl = "",
                                streamTitle = "",
                                viewers = ui.viewers,
                                categoryName = ui.name,
                                bannerUrl = ui.bannerUrl
                            )
                        )
                    }
                }
            }

            streamerJobs.awaitAll()
            categoryJobs.awaitAll()

            _isRefreshing.update { false }
        }
    }
}
