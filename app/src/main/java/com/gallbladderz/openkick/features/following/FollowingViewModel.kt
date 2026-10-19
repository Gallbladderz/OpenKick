/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.data.local.FollowType
import com.gallbladderz.openkick.core.data.local.FollowedEntity
import com.gallbladderz.openkick.core.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    data class Error(val message: UiText) : FollowingUiState
}

class FollowingViewModel(
    private val followingRepository: FollowingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val channelDetails = MutableStateFlow<Map<String, FollowedStreamerUi>>(emptyMap())
    private val categoryDetails = MutableStateFlow<Map<String, FollowedCategoryUi>>(emptyMap())
    private var syncJob: Job? = null
    private var loadError: UiText? = null

    init {
        observeFollows()
        viewModelScope.launch {
            combine(
                sessionManager.mode,
                sessionManager.sessionGeneration
            ) { mode, generation -> mode to generation }
                .collectLatest {
                    syncJob?.cancel()
                    loadError = null
                    _uiState.value = FollowingUiState.Loading
                    syncWithApi()
                }
        }
        viewModelScope.launch {
            refreshTrigger.collect {
                syncJob?.cancel()
                syncWithApi()
            }
        }
    }

    fun unfollowStreamer(slug: String) {
        viewModelScope.launch {
            try {
                followingRepository.toggleStreamerFollow(slug, true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.message?.let { message -> UiText.DynamicString(message) }
                    ?: UiText.StringResource(R.string.could_not_load_subscriptions)
                _uiState.value = FollowingUiState.Error(
                    loadError ?: UiText.StringResource(R.string.could_not_load_subscriptions)
                )
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        refreshTrigger.tryEmit(Unit)
    }

    private fun observeFollows() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                followingRepository.followedEntities(),
                channelDetails,
                categoryDetails
            ) { entities, details, categories ->
                toUiState(entities, details, categories)
            }.collectLatest { state ->
                if (loadError == null) _uiState.value = state
            }
        }
    }

    private fun toUiState(
        entities: List<FollowedEntity>,
        details: Map<String, FollowedStreamerUi>,
        categoriesDetails: Map<String, FollowedCategoryUi>
    ): FollowingUiState.Success {
        val categories = entities.filter { it.type == FollowType.CATEGORY }.map {
            val detail = categoriesDetails[it.slug.lowercase()]
            FollowedCategoryUi(
                it.slug,
                detail?.name?.takeIf { name -> name.isNotBlank() }
                    ?: it.categoryName.ifBlank { it.slug },
                detail?.bannerUrl?.takeIf { url -> url.isNotBlank() } ?: it.bannerUrl,
                detail?.viewers ?: it.viewers
            )
        }.sortedByDescending { it.viewers }
        val streamers = entities.filter { it.type == FollowType.STREAMER }.map {
            val channel = details[it.slug.lowercase()]
            FollowedStreamerUi(
                slug = it.slug,
                username = channel?.username?.takeIf { name -> name.isNotBlank() } ?: it.username,
                avatarUrl = channel?.avatarUrl?.takeIf { url -> url.isNotBlank() } ?: it.avatarUrl,
                isLive = channel?.isLive ?: it.isLive,
                streamTitle = channel?.streamTitle?.takeIf { title -> title.isNotBlank() }
                    ?: it.streamTitle,
                viewers = channel?.viewers ?: it.viewers,
                categoryName = channel?.categoryName?.takeIf { name -> name.isNotBlank() }
                    ?: it.categoryName,
                streamThumbnailUrl = channel?.streamThumbnailUrl?.takeIf { url -> url.isNotBlank() }
                    ?: it.bannerUrl
            )
        }
        return FollowingUiState.Success(
            streamers.filter { it.isLive }.sortedByDescending { it.viewers },
            streamers.filter { !it.isLive }.sortedBy { it.username.lowercase() },
            categories
        )
    }

    private fun syncWithApi() {
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.update { true }
            channelDetails.value = emptyMap()
            categoryDetails.value = emptyMap()
            try {
                followingRepository.refreshFollowing()
                loadError = null
                val entities = followingRepository.followedEntities().first()
                _uiState.value = toUiState(
                    entities,
                    channelDetails.value,
                    categoryDetails.value
                )
                val streamerSlugs =
                    entities.filter { it.type == FollowType.STREAMER }.map { it.slug }
                val categorySlugs =
                    entities.filter { it.type == FollowType.CATEGORY }.map { it.slug }

                val streamerJobs = streamerSlugs.map { slug ->
                    async {
                        followingRepository.fetchChannelDetails(slug).getOrNull()?.let { ui ->
                            channelDetails.update { it + (slug.lowercase() to ui) }
                            if (!followingRepository.isAuthenticated) {
                                followingRepository.saveLocalSnapshot(
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
                }

                val categoryJobs = categorySlugs.map { slug ->
                    async {
                        followingRepository.fetchCategoryDetails(slug).getOrNull()?.let { ui ->
                            categoryDetails.update { it + (slug.lowercase() to ui) }
                            if (!followingRepository.isAuthenticated) {
                                followingRepository.saveLocalSnapshot(
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
                }

                streamerJobs.awaitAll()
                categoryJobs.awaitAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.message?.let { message -> UiText.DynamicString(message) }
                    ?: UiText.StringResource(R.string.could_not_load_subscriptions)
                _uiState.value = FollowingUiState.Error(
                    loadError ?: UiText.StringResource(R.string.could_not_load_subscriptions)
                )
            } finally {
                if (syncJob === coroutineContext[Job]) _isRefreshing.value = false
            }
        }
    }
}
