/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R

@Composable
internal fun KickSignInPrompt(
    onContinue: () -> Unit,
    content: @Composable (requestSignIn: () -> Unit) -> Unit
) {
    var showInstruction by rememberSaveable { mutableStateOf(false) }
    content { showInstruction = true }

    if (showInstruction) {
        AlertDialog(
            onDismissRequest = { showInstruction = false },
            icon = {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text(stringResource(R.string.kick_login_instruction_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        R.string.kick_login_instruction_open,
                        R.string.kick_login_instruction_profile,
                        R.string.kick_login_instruction_return
                    ).forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${index + 1}.",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(AnnotatedString.fromHtml(stringResource(step)), Modifier.weight(1f))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstruction = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showInstruction = false
                    onContinue()
                }) {
                    Text(stringResource(R.string.kick_login_instruction_continue))
                }
            }
        )
    }
}
