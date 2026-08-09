/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R

@Composable
fun AboutAppRoute(
    onBackClick: () -> Unit,
    onLicensesClick: () -> Unit
) {
    AboutAppScreen(
        onBackClick = onBackClick,
        onLicensesClick = onLicensesClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit,
    onLicensesClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: "Unknown"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_app)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_about_cat),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            )

            SettingsGroupHeader(stringResource(R.string.links))

            ListItem(
                headlineContent = { Text(stringResource(R.string.open_source_licenses)) },
                modifier = Modifier.clickable(onClick = onLicensesClick)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.github)) },
                modifier = Modifier.clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Gallbladderz/OpenKick")
                    )
                    context.startActivity(intent)
                }
            )

            SettingsGroupHeader(stringResource(R.string.donate))

            val wallets = listOf(
                "GRAM" to "UQCe8z8g1DDZv_kXwTmC1nHthbU9QFQFcPLoUApTT9Osnvix",
                "USDT (TRC-20)" to "TP4jKX8hbLVd87cVF5ZkjuHz1jc5oxphge",
                "USDT (BEP-20)" to "0x2325718CD1dCD1fA3463d8Ff7512B891bDe11Edf"
            )

            val copiedText = stringResource(R.string.copied)

            wallets.forEach { (name, address) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(address) },
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(address))
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
