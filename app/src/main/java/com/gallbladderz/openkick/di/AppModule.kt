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

val appModule = module {
    single { SettingsRepository(androidContext()) }
    single { okhttp3.OkHttpClient() }

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

    single { HomeRepository() }

    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { CategoriesViewModel(get()) } // <-- ДОБАВИЛИ get()
    viewModel { SearchViewModel(get()) }     // <-- ДОБАВИЛИ get()
}