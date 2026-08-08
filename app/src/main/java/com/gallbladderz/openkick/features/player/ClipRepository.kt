package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.features.home.ClipUiModel
import java.util.concurrent.ConcurrentHashMap

class ClipRepository {
    private val clipCache = ConcurrentHashMap<String, ClipUiModel>()

    fun cacheClips(clips: List<ClipUiModel>) {
        clips.forEach { clipCache[it.id] = it }
    }

    fun cacheClip(clip: ClipUiModel) {
        clipCache[clip.id] = clip
    }

    fun getClipById(id: String): ClipUiModel? = clipCache[id]
}
