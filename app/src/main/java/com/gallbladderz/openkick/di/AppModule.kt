package com.gallbladderz.openkick.di

import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.home.HomeRepository
import com.gallbladderz.openkick.features.home.HomeViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SettingsRepository(androidContext()) }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                url("https://kick.com/api/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept", "application/json")
            }
        }
    }

    single { HomeRepository(get()) }

    viewModel { MainViewModel(get()) }
    viewModel { HomeViewModel(get()) }
}