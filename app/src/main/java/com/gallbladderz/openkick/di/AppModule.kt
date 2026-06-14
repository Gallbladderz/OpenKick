package com.gallbladderz.openkick.di

import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.profile.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SettingsRepository(androidContext()) }
    viewModel { MainViewModel(get()) }
}