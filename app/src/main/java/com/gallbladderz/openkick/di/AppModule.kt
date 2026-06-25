@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class) 

package com.gallbladderz.openkick.di

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.room.Room
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.core.network.MobileHeadersInterceptor
import com.gallbladderz.openkick.data.local.AppDatabase
import com.gallbladderz.openkick.data.local.FollowsRepository
import com.gallbladderz.openkick.features.categories.CategoriesRepository
import com.gallbladderz.openkick.features.categories.CategoriesViewModel
import com.gallbladderz.openkick.features.categories.CategoryDetailsViewModel
import com.gallbladderz.openkick.features.following.FollowingRepository
import com.gallbladderz.openkick.features.following.FollowingViewModel
import com.gallbladderz.openkick.features.home.HomeRepository
import com.gallbladderz.openkick.features.home.HomeViewModel
import com.gallbladderz.openkick.features.notifications.StreamCheckWorker
import com.gallbladderz.openkick.features.player.ChatRepository
import com.gallbladderz.openkick.features.player.PlayerManager
import com.gallbladderz.openkick.features.player.PlayerRepository
import com.gallbladderz.openkick.features.player.PlayerViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import com.gallbladderz.openkick.features.profile.StreamerProfileViewModel
import com.gallbladderz.openkick.features.search.SearchRepository
import com.gallbladderz.openkick.features.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val appModule = module {
    single { SettingsRepository(androidContext()) }
    workerOf(::StreamCheckWorker)

    single {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(MobileHeadersInterceptor())
            .build()
    }

    single { HomeRepository(get()) }
    single { SearchRepository(get()) }
    single { CategoriesRepository(get()) }
    single { PlayerRepository(get()) }
    single { ChatRepository(get()) }
    single { FollowingRepository(get()) }

    
    single { StreamerProfileRepository(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "openkick_db"
        ).build()
    }

    single { get<AppDatabase>().followsDao() }
    single { FollowsRepository(get()) }

    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }

    single<DataSource.Factory> {
        DefaultHttpDataSource.Factory()
            .setUserAgent(KickApiConstants.USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to "https://kick.com",
                    "Referer" to "https://kick.com/"
                )
            )
    }

    factory { PlayerManager(androidContext(), get()) }

    viewModel { PlayerViewModel(get(), get(), get(), get()) }
    viewModel { CategoriesViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { CategoryDetailsViewModel(get()) }
    viewModel { FollowingViewModel(get(), get()) }


    viewModel { StreamerProfileViewModel(get(), get()) }
}