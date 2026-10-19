/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */


@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gallbladderz.openkick.di

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.room.Room
import com.gallbladderz.openkick.BuildConfig
import com.gallbladderz.openkick.core.auth.OAuthManager
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.data.local.AppDatabase
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.network.AuthInterceptor
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.core.network.MobileHeadersInterceptor
import com.gallbladderz.openkick.core.network.api.AccountApi
import com.gallbladderz.openkick.core.network.api.CategoryFollowingApi
import com.gallbladderz.openkick.core.network.api.ChatEmotesApi
import com.gallbladderz.openkick.core.network.api.ChatSendApi
import com.gallbladderz.openkick.core.network.api.FollowingApi
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.network.api.OAuthApi
import com.gallbladderz.openkick.core.network.api.RealtimeApi
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.features.categories.CategoriesRepository
import com.gallbladderz.openkick.features.categories.CategoriesViewModel
import com.gallbladderz.openkick.features.categories.CategoryDetailsViewModel
import com.gallbladderz.openkick.features.following.FollowingRepository
import com.gallbladderz.openkick.features.following.FollowingViewModel
import com.gallbladderz.openkick.features.following.LocalFollowingDataSource
import com.gallbladderz.openkick.features.following.RemoteCategoryFollowingDataSource
import com.gallbladderz.openkick.features.following.RemoteFollowingDataSource
import com.gallbladderz.openkick.features.home.HomeRepository
import com.gallbladderz.openkick.features.home.HomeViewModel
import com.gallbladderz.openkick.features.notifications.StreamCheckWorker
import com.gallbladderz.openkick.features.player.ChatRepository
import com.gallbladderz.openkick.features.player.ClipPlayerViewModel
import com.gallbladderz.openkick.features.player.PlayerRepository
import com.gallbladderz.openkick.features.player.PlayerViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import com.gallbladderz.openkick.features.profile.StreamerProfileViewModel
import com.gallbladderz.openkick.features.search.SearchRepository
import com.gallbladderz.openkick.features.search.SearchViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val appModule = module {
    single<SettingsRepository> { SettingsRepository(androidContext()) }
    single<SessionManager> { SessionManager(androidContext()) }
    single<Retrofit>(named("bffRetrofit")) {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        Retrofit.Builder().baseUrl(BuildConfig.BFF_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<OAuthApi> { get<Retrofit>(named("bffRetrofit")).create(OAuthApi::class.java) }
    single<OAuthManager> { OAuthManager(androidContext(), get<OAuthApi>(), get<SessionManager>()) }
    single<okhttp3.OkHttpClient>(named("authClient")) {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(get<SessionManager>(), get<OAuthManager>()))
            .build()
    }
    single<FollowingApi> {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        Retrofit.Builder().baseUrl(BuildConfig.BFF_BASE_URL)
            .client(get<okhttp3.OkHttpClient>(named("authClient")))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(FollowingApi::class.java)
    }
    single<RemoteFollowingDataSource> {
        RemoteFollowingDataSource(
            get<FollowingApi>(),
            get<SessionManager>()
        )
    }
    single<CategoryFollowingApi> {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        Retrofit.Builder()
            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(get<okhttp3.OkHttpClient>(named("authClient")))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(CategoryFollowingApi::class.java)
    }
    single<RemoteCategoryFollowingDataSource> {
        RemoteCategoryFollowingDataSource(
            get<CategoryFollowingApi>(),
            get<SessionManager>()
        )
    }
    single<LocalFollowingDataSource> { LocalFollowingDataSource(get<com.gallbladderz.openkick.core.data.local.FollowsDao>()) }
    workerOf(::StreamCheckWorker)

    single<okhttp3.OkHttpClient> {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(MobileHeadersInterceptor())
            .build()
    }

    single<Retrofit> {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()

            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(get<okhttp3.OkHttpClient>())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single<KickApiService> { get<Retrofit>().create(KickApiService::class.java) }
    single<KickApiService>(named("authKickApi")) {
        Retrofit.Builder()
            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(get<okhttp3.OkHttpClient>(named("authClient")))
            .addConverterFactory(Json {
                ignoreUnknownKeys = true
            }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KickApiService::class.java)
    }
    single<AccountApi> {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(get<okhttp3.OkHttpClient>(named("authClient")))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(AccountApi::class.java)
    }

    single<ChatSendApi> {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(get<okhttp3.OkHttpClient>(named("authClient")))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ChatSendApi::class.java)
    }

    single<ChatEmotesApi> {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(KickApiConstants.KICK_BASE_URL + "/")
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(get<SessionManager>(), get<OAuthManager>()))
                    .addInterceptor(MobileHeadersInterceptor())
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ChatEmotesApi::class.java)
    }

    single<RealtimeApi> { get<Retrofit>().create(RealtimeApi::class.java) }

    single<com.gallbladderz.openkick.core.data.repository.ClipRepository> {
        com.gallbladderz.openkick.core.data.repository.ClipRepository(
            get<KickApiService>()
        )
    }

    single<CategoriesRepository> {
        CategoriesRepository(
            get<KickApiService>(),
            get<com.gallbladderz.openkick.core.data.repository.ClipRepository>()
        )
    }

    single<HomeRepository> {
        HomeRepository(
            get<KickApiService>(),
            get<com.gallbladderz.openkick.core.data.repository.ClipRepository>()
        )
    }
    single<SearchRepository> { SearchRepository(get<KickApiService>()) }
    single<PlayerRepository> {
        PlayerRepository(
            get<KickApiService>(),
            get<KickApiService>(named("authKickApi")),
            get<SessionManager>()
        )
    }
    single<ChatRepository> {
        ChatRepository(
            androidContext(),
            get<okhttp3.OkHttpClient>(),
            get<RealtimeApi>(),
            get<ChatSendApi>(),
            get<ChatEmotesApi>(),
            get<SessionManager>(),
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }
    single<FollowingRepository> {
        FollowingRepository(
            get<KickApiService>(),
            get<LocalFollowingDataSource>(),
            get<RemoteFollowingDataSource>(),
            get<RemoteCategoryFollowingDataSource>(),
            get<SessionManager>()
        )
    }
    single<StreamerProfileRepository> {
        StreamerProfileRepository(
            get<KickApiService>(),
            get<com.gallbladderz.openkick.core.data.repository.ClipRepository>()
        )
    }

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "openkick_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single<com.gallbladderz.openkick.core.data.local.FollowsDao> { get<AppDatabase>().followsDao() }

    viewModel { MainViewModel(get<SettingsRepository>()) }
    viewModel { HomeViewModel(get<HomeRepository>(), get<SettingsRepository>()) }

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

    single<PlayerManager> { PlayerManager(androidContext(), get<DataSource.Factory>()) }

    viewModel {
        PlayerViewModel(
            get<PlayerRepository>(),
            get<ChatRepository>(),
            get<FollowingRepository>(),
            get<PlayerManager>(),
            get<SessionManager>()
        )
    }
    viewModel {
        ClipPlayerViewModel(
            get<com.gallbladderz.openkick.core.data.repository.ClipRepository>(),
            androidContext()
        )
    }
    viewModel { CategoriesViewModel(get<CategoriesRepository>(), get<FollowingRepository>()) }
    viewModel { SearchViewModel(get<SearchRepository>()) }

    viewModel { CategoryDetailsViewModel(get<CategoriesRepository>(), get<FollowingRepository>()) }
    viewModel { FollowingViewModel(get<FollowingRepository>(), get<SessionManager>()) }
    viewModel {
        StreamerProfileViewModel(
            get<StreamerProfileRepository>(),
            get<FollowingRepository>(),
            get<com.gallbladderz.openkick.core.data.repository.ClipRepository>()
        )
    }
    viewModel { (videoId: String) ->
        com.gallbladderz.openkick.features.player.vod.VodPlayerViewModel(
            videoId = videoId,
            context = androidContext(),
            dataSourceFactory = get(),
            repository = get()
        )
    }
}
