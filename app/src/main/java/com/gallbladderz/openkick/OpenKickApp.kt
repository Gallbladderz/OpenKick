package com.gallbladderz.openkick

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gallbladderz.openkick.di.appModule
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenKickApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OpenKickApp)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { get<OkHttpClient>() }
            .crossfade(true)
            .build()
    }
}