package com.gallbladderz.openkick

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gallbladderz.openkick.di.appModule
import com.gallbladderz.openkick.features.notifications.StreamCheckWorker
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class OpenKickApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@OpenKickApp)
            workManagerFactory() 
            modules(appModule)
        }

        
        setupStreamNotifications()
    }

    private fun setupStreamNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) 
            .build()

        val workRequest = PeriodicWorkRequestBuilder<StreamCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StreamCheckWork",
            ExistingPeriodicWorkPolicy.KEEP, 
            workRequest
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { get<OkHttpClient>() }
            .crossfade(true)
            .build()
    }
}