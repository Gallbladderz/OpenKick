/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.domain.model.StreamUiModel
import com.gallbladderz.openkick.core.ui.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val streams: List<StreamUiModel>,
        val clips: List<ClipUiModel>
    ) : HomeUiState

    data class Error(val message: UiText) : HomeUiState
}

private data class CombinedState(
    val langs: Set<String>,
    val hideSlots: Boolean,
    val hidePools: Boolean,
    val hideCrypto: Boolean
)

class HomeViewModel(
    private val repository: HomeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var streamsCursor: String? = null
    private var clipsCursor: String? = null
    private var isClipsLoading = false
    private var isClipsEnd = false
    private var homeLoadJob: Job? = null
    private var streamsPageJob: Job? = null

    private var hiddenCategories: Set<String> = emptySet()

    private val _sort = MutableStateFlow("viewer_count_desc")
    val sort = _sort.asStateFlow()

    private val _isGridMode = MutableStateFlow(false)
    val isGridMode = _isGridMode.asStateFlow()

    private val _selectedFilterLanguages = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilterLanguages = _selectedFilterLanguages.asStateFlow()

    fun updateFiltersAndRefresh(newSort: String, newLangs: Set<String>) {
        _sort.update { newSort }
        _selectedFilterLanguages.update { newLangs }
        viewModelScope.launch {
            settingsRepository.setHomeStreamSort(newSort)
        }
        fetchHomeData()
    }

    fun setGridMode(isGrid: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeGridMode(isGrid)
        }
    }

    private suspend fun fetchVisibleStreams(
        cursor: String? = null,
        existingIds: Set<String> = emptySet()
    ): Result<Pair<List<StreamUiModel>, String?>> {
        val sort = _sort.value
        val languages = _selectedFilterLanguages.value
        val hidden = hiddenCategories
        val minimumStreams = if (hidden.isEmpty()) 1 else if (cursor == null) 12 else 6
        return loadVisibleStreamPages(cursor, hidden, minimumStreams, existingIds) { after ->
            repository.fetchLivestreams(cursor = after, sort = sort, languages = languages)
        }
    }

    init {
        viewModelScope.launch {
            settingsRepository.homeGridModeFlow.collectLatest { gridMode ->
                _isGridMode.value = gridMode
            }
        }
        viewModelScope.launch {
            settingsRepository.homeStreamSortFlow.collectLatest { storedSort ->
                _sort.value = storedSort
            }
        }
        viewModelScope.launch {

            kotlinx.coroutines.flow.combine(
                settingsRepository.selectedLanguagesFlow,
                settingsRepository.hideSlotsFlow,
                settingsRepository.hidePoolsFlow,
                settingsRepository.hideCryptoFlow
            ) { langs, hideSlots, hidePools, hideCrypto ->
                CombinedState(langs, hideSlots, hidePools, hideCrypto)
            }.distinctUntilChanged().collectLatest { state ->
                if (_selectedFilterLanguages.value.isEmpty()) {
                    _selectedFilterLanguages.update { state.langs }
                }
                hiddenCategories = buildSet {
                    if (state.hideSlots) add("slots")
                    if (state.hidePools) add("pools-hot-tubs-bikinis")
                    if (state.hideCrypto) add("crypto-and-trading")
                }
                fetchHomeData()
            }
        }
    }

    fun fetchHomeData() {
        homeLoadJob?.cancel()
        streamsPageJob?.cancel()

        _uiState.update { HomeUiState.Loading }
        streamsCursor = null
        clipsCursor = null
        isClipsEnd = false

        homeLoadJob = viewModelScope.launch {
            try {
                val streamsDeferred = async { fetchVisibleStreams() }
                val clipsDeferred = async { repository.fetchTopClips(null) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                val streamsPair = streamsResult.getOrNull()
                val streamsList = streamsPair?.first ?: emptyList()
                streamsCursor = streamsPair?.second

                val clipsPair = clipsResult.getOrNull()
                val clipsList = clipsPair?.first ?: emptyList()
                clipsCursor = clipsPair?.second

                if (streamsResult.isFailure && clipsResult.isFailure) {
                    val ex = streamsResult.exceptionOrNull() ?: clipsResult.exceptionOrNull()
                    _uiState.update {
                        HomeUiState.Error(ex?.message?.let { UiText.DynamicString(it) }
                            ?: UiText.StringResource(R.string.total_failure))
                    }
                } else {

                    _uiState.update {
                        HomeUiState.Success(
                            streams = streamsList,
                            clips = clipsList
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    HomeUiState.Error(e.message?.let { UiText.DynamicString(it) }
                        ?: UiText.StringResource(R.string.unexpected_error))
                }
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return

        homeLoadJob?.cancel()
        streamsPageJob?.cancel()
        _isRefreshing.update { true }

        homeLoadJob = viewModelScope.launch {
            try {
                val streamsDeferred = async { fetchVisibleStreams() }
                val clipsDeferred = async { repository.fetchTopClips(null) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                val streamsPair = streamsResult.getOrNull()
                val clipsPair = clipsResult.getOrNull()

                if (streamsPair != null || clipsPair != null) {
                    if (streamsPair != null) streamsCursor = streamsPair.second
                    if (clipsPair != null) {
                        clipsCursor = clipsPair.second
                        isClipsEnd = false
                    }

                    _uiState.update { state ->
                        val previous = state as? HomeUiState.Success
                        HomeUiState.Success(
                            streams = streamsPair?.first ?: previous?.streams.orEmpty(),
                            clips = clipsPair?.first ?: previous?.clips.orEmpty()
                        )
                    }
                }
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    fun loadMoreStreams() {
        val cursor = streamsCursor ?: return
        val current = _uiState.value as? HomeUiState.Success ?: return
        if (streamsPageJob?.isActive == true || homeLoadJob?.isActive == true) return

        streamsPageJob = viewModelScope.launch {
            val existingIds = current.streams.mapTo(mutableSetOf()) { it.id }
            val (newStreams, nextCursor) = fetchVisibleStreams(cursor, existingIds).getOrNull()
                ?: return@launch
            streamsCursor = nextCursor
            _uiState.update { state ->
                if (state is HomeUiState.Success) state.copy(streams = state.streams + newStreams)
                else state
            }
        }
    }

    fun loadMoreClips() {
        if (isClipsLoading || isClipsEnd || _uiState.value !is HomeUiState.Success) {
            return
        }

        isClipsLoading = true

        viewModelScope.launch {
            val result = repository.fetchTopClips(cursor = clipsCursor)

            if (result.isSuccess) {
                val (newClips, nextCursor) = result.getOrThrow()
                clipsCursor = nextCursor

                if (newClips.isEmpty()) {
                    isClipsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isClipsEnd = true
                    }
                    _uiState.update { state ->
                        if (state is HomeUiState.Success) {
                            val merged = (state.clips + newClips).distinctBy { it.id }
                            state.copy(clips = merged)
                        } else state
                    }
                }
            } else {
                isClipsEnd = true
            }
            isClipsLoading = false
        }
    }
}
