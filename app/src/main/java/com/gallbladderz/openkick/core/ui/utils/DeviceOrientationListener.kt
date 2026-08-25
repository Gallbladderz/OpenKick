/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.ui.utils

import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class DeviceOrientation {
    PORTRAIT,
    LANDSCAPE,
    UNKNOWN
}

@Composable
fun rememberDeviceOrientation(): DeviceOrientation {
    val context = LocalContext.current
    var orientation by remember { mutableStateOf(DeviceOrientation.UNKNOWN) }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(angle: Int) {
                if (angle == ORIENTATION_UNKNOWN) return

                val isAutoRotateEnabled = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                ) == 1

                if (!isAutoRotateEnabled) return

                val newOrientation = when {
                    (angle in 0..20) || (angle in 340..359) || (angle in 160..200) -> DeviceOrientation.PORTRAIT
                    (angle in 70..110) || (angle in 250..290) -> DeviceOrientation.LANDSCAPE
                    else -> null
                }

                if (newOrientation != null && newOrientation != orientation) {
                    orientation = newOrientation
                }
            }
        }
        listener.enable()
        onDispose {
            listener.disable()
        }
    }

    return orientation
}
