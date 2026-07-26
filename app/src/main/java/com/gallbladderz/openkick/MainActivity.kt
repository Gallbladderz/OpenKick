package com.gallbladderz.openkick

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.notifications.StreamKeepaliveService
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.ui.theme.OpenKickTheme
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
            val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {

                if (settingsRepository.backgroundKeepaliveFlow.first()) {
                    ContextCompat.startForegroundService(
                        this@MainActivity,
                        Intent(this@MainActivity, StreamKeepaliveService::class.java)
                    )
                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            OpenKickTheme(appTheme = appTheme, useDynamicColors = useDynamicColors) {
                OpenKickNavHost()
            }
        }
    }
}