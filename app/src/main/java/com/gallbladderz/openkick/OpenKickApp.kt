package com.gallbladderz.openkick

import android.app.Application
import com.gallbladderz.openkick.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenKickApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OpenKickApp)
            modules(appModule)
        }
    }
}