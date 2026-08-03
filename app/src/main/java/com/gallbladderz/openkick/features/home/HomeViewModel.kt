package com.gallbladderz.openkick.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.UiText

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
    private var isStreamsLoading = false
    private var isClipsLoading = false
    private var isStreamsEnd = false
    private var isClipsEnd = false

    private var currentLanguages: Set<String>? = null
    private var currentHideSlots: Boolean = false
    private var currentHidePools: Boolean = false
    private var currentHideCrypto: Boolean = false

    private val _sort = MutableStateFlow("viewer_count_desc")
    val sort = _sort.asStateFlow()

    private val _isGridMode = MutableStateFlow(false)
    val isGridMode = _isGridMode.asStateFlow()

    private val _selectedFilterLanguages = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilterLanguages = _selectedFilterLanguages.asStateFlow()

    fun updateFiltersAndRefresh(newSort: String, newLangs: Set<String>) {
        _sort.value = newSort
        _selectedFilterLanguages.value = newLangs
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


    private fun filterBanned(streams: List<StreamUiModel>): List<StreamUiModel> {
        if (!currentHideSlots && !currentHidePools && !currentHideCrypto) return streams

        val bannedSlugs = mutableSetOf<String>()
        if (currentHideSlots) bannedSlugs.add("slots")
        if (currentHidePools) bannedSlugs.add("pools-hot-tubs-bikinis")
        if (currentHideCrypto) bannedSlugs.add("crypto-and-trading")

        return streams.filter { stream ->
            stream.categorySlug.lowercase() !in bannedSlugs
        }
    }


    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.homeGridModeFlow.collect {
                _isGridMode.value = it
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.homeStreamSortFlow.collect {
                _sort.value = it
            }
        }
        viewModelScope.launch(Dispatchers.IO) {

            kotlinx.coroutines.flow.combine(
                settingsRepository.selectedLanguagesFlow,
                settingsRepository.hideSlotsFlow,
                settingsRepository.hidePoolsFlow,
                settingsRepository.hideCryptoFlow
            ) { langs, hideSlots, hidePools, hideCrypto ->
                CombinedState(langs, hideSlots, hidePools, hideCrypto)
            }.collect { state ->
                val langsChanged = currentLanguages != state.langs
                val hideSlotsChanged = currentHideSlots != state.hideSlots
                val hidePoolsChanged = currentHidePools != state.hidePools
                val hideCryptoChanged = currentHideCrypto != state.hideCrypto


                if (langsChanged || hideSlotsChanged || hidePoolsChanged || hideCryptoChanged || currentLanguages == null) {
                    currentLanguages = state.langs
                    if (_selectedFilterLanguages.value.isEmpty()) {
                        _selectedFilterLanguages.value = state.langs
                    }
                    currentHideSlots = state.hideSlots
                    currentHidePools = state.hidePools
                    currentHideCrypto = state.hideCrypto
                    fetchHomeData()
                }
            }
        }
    }

    fun fetchHomeData() {
        val langs = _selectedFilterLanguages.value

        _uiState.value = HomeUiState.Loading
        streamsCursor = null
        clipsCursor = null
        isStreamsEnd = false
        isClipsEnd = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streamsDeferred = async {
                    repository.fetchLivestreams(cursor = null, sort = _sort.value, languages = langs)
                }
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
                    _uiState.value =
                        HomeUiState.Error(ex?.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.total_failure))
                } else {

                    val filteredStreams = filterBanned(streamsList)
                    _uiState.value = HomeUiState.Success(
                        streams = filteredStreams,
                        clips = clipsList
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.unexpected_error))
            }
        }
    }

    fun refresh() {
        val langs = _selectedFilterLanguages.value
        if (_isRefreshing.value) return

        _isRefreshing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streamsDeferred = async {
                    repository.fetchLivestreams(cursor = null, sort = _sort.value, languages = langs)
                }
                val clipsDeferred = async { repository.fetchTopClips(null) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                val streamsPair = streamsResult.getOrNull()
                val clipsPair = clipsResult.getOrNull()

                if (streamsPair != null || clipsPair != null) {
                    streamsCursor = streamsPair?.second ?: streamsCursor
                    clipsCursor = clipsPair?.second ?: clipsCursor

                    isStreamsEnd = false
                    isClipsEnd = false

                    _uiState.value = HomeUiState.Success(

                        streams = filterBanned(streamsPair?.first ?: emptyList()),
                        clips = clipsPair?.first ?: emptyList()
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreStreams() {
        val langs = _selectedFilterLanguages.value

        if (isStreamsLoading || isStreamsEnd || _uiState.value !is HomeUiState.Success) {
            return
        }

        isStreamsLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchLivestreams(cursor = streamsCursor, sort = _sort.value, languages = langs)

            if (result.isSuccess) {
                val (newStreams, nextCursor) = result.getOrThrow()
                streamsCursor = nextCursor


                val filteredNewStreams = filterBanned(newStreams)

                if (newStreams.isEmpty()) {
                    isStreamsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isStreamsEnd = true
                    }
                    val currentState = _uiState.value as HomeUiState.Success

                    val merged = (currentState.streams + filteredNewStreams).distinctBy { it.id }
                    _uiState.value = currentState.copy(streams = merged)
                }
            } else {
                isStreamsEnd = true
            }
            isStreamsLoading = false
        }
    }

    fun loadMoreClips() {
        if (isClipsLoading || isClipsEnd || _uiState.value !is HomeUiState.Success) {
            return
        }

        isClipsLoading = true

        viewModelScope.launch(Dispatchers.IO) {
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
                    val currentState = _uiState.value as HomeUiState.Success
                    val merged = (currentState.clips + newClips).distinctBy { it.id }
                    _uiState.value = currentState.copy(clips = merged)
                }
            } else {
                isClipsEnd = true
            }
            isClipsLoading = false
        }
    }
}