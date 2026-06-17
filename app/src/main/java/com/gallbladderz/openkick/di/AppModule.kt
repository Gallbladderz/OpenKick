package com.gallbladderz.openkick.di

import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.categories.CategoriesViewModel
import com.gallbladderz.openkick.features.search.SearchViewModel
import com.gallbladderz.openkick.features.home.HomeRepository
import com.gallbladderz.openkick.features.home.HomeViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.gallbladderz.openkick.features.player.PlayerViewModel
import com.gallbladderz.openkick.features.player.PlayerRepository
import com.gallbladderz.openkick.features.search.SearchRepository
import com.gallbladderz.openkick.features.categories.CategoriesRepository
import com.gallbladderz.openkick.core.network.MobileHeadersInterceptor

val appModule = module {
    single { SettingsRepository(androidContext()) }

    single {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(MobileHeadersInterceptor())
            .build()
    }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    single { HomeRepository(get()) }
    single { SearchRepository(get()) }
    single { CategoriesRepository(get()) }
    single { PlayerRepository(get()) }

    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { PlayerViewModel(get(), get()) }
    viewModel { CategoriesViewModel(get()) } // <-- ДОБАВИЛИ get()
    viewModel { SearchViewModel(get()) }     // <-- ДОБАВИЛИ get()
}