package com.gallbladderz.openkick.di

import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.categories.CategoriesViewModel
import com.gallbladderz.openkick.features.search.SearchViewModel
import com.gallbladderz.openkick.features.home.HomeRepository
import com.gallbladderz.openkick.features.home.HomeViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.features.profile.FollowingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.gallbladderz.openkick.features.player.PlayerViewModel
import com.gallbladderz.openkick.features.player.PlayerRepository
import com.gallbladderz.openkick.features.search.SearchRepository
import com.gallbladderz.openkick.features.categories.CategoriesRepository
import com.gallbladderz.openkick.core.network.MobileHeadersInterceptor
import com.gallbladderz.openkick.features.categories.CategoryDetailsViewModel

val appModule = module {
    single { SettingsRepository(androidContext()) }

    single {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(MobileHeadersInterceptor())
            .build()
    }

    single { HomeRepository(get()) }
    single { SearchRepository(get()) }
    single { CategoriesRepository(get()) }
    single { PlayerRepository(get()) }

    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { PlayerViewModel(get(), get(), get()) } // 🔥 ТРИ ГЕТА, МАТЬ ИХ!
    viewModel { CategoriesViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { FollowingViewModel(get(), get()) }
    viewModel { CategoryDetailsViewModel(get()) }
}