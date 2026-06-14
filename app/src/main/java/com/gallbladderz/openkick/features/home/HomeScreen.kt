package com.gallbladderz.openkick.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Network Test (Ktor)", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.loadData() }) {
            Text("Fetch Kick API")
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (val uiState = state) {
            is HomeUiState.Idle -> {
                Text(text = "Press the button to test Kick API...", textAlign = TextAlign.Center)
            }
            is HomeUiState.Loading -> {
                CircularProgressIndicator()
            }
            is HomeUiState.Success -> {
                val playbackUrl = uiState.data.playbackUrl
                if (!playbackUrl.isNullOrBlank()) {
                    KickStreamPlayer(
                        videoUrl = playbackUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                } else {
                    Text(text = "Offline", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                }
            }
            is HomeUiState.Error -> {
                Text(text = "Error:\n${uiState.message}", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}