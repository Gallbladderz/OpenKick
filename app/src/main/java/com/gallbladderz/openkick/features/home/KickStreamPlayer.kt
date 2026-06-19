package com.gallbladderz.openkick.features.home

import android.webkit.WebSettings
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun KickStreamPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(videoUrl) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            val defaultUserAgent = WebSettings.getDefaultUserAgent(context).replace("; wv", "")

            withContext(Dispatchers.Main) {
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(defaultUserAgent)
                    .setDefaultRequestProperties(
                        mapOf(
                            "Origin" to "https://kick.com",
                            "Referer" to "https://kick.com/"
                        )
                    )

                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }

        onDispose {
            job.cancel()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}
