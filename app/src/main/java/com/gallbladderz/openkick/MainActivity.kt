package com.gallbladderz.openkick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gallbladderz.openkick.core.ui.theme.OpenKickTheme
import com.gallbladderz.openkick.navigation.OpenKickNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем рисование под системными панелями
        enableEdgeToEdge()

        setContent {
            OpenKickTheme {
                // Вся навигация и UI живут здесь
                OpenKickNavHost()
            }
        }
    }
}