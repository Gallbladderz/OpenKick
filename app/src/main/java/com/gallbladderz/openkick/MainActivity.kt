package com.gallbladderz.openkick

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity 
import androidx.compose.runtime.LaunchedEffect
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.ui.theme.OpenKickTheme
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.features.notifications.StreamKeepaliveService
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

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

            OpenKickTheme {
                OpenKickNavHost()
            }
        }
    }
}