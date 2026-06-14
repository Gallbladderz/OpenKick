package com.gallbladderz.openkick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.ui.theme.OpenKickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenKickTheme {
                OpenKickNavHost()
            }
        }
    }
}