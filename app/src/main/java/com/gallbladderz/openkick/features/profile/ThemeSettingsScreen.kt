package com.gallbladderz.openkick.features.profile

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.shifthackz.catppuccin.palette.Catppuccin
import com.gallbladderz.openkick.core.datastore.AppTheme
import com.gallbladderz.openkick.core.datastore.AppAccent
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsRoute(
    onBackClick: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
    val appAccent by mainViewModel.appAccent.collectAsStateWithLifecycle()
    val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

    ThemeSettingsScreen(
        appTheme = appTheme,
        appAccent = appAccent,
        onUpdateAccent = { mainViewModel.updateAppAccent(it) },
        useDynamicColors = useDynamicColors,
        onUpdateTheme = { mainViewModel.updateAppTheme(it) },
        onUpdateDynamicColors = { mainViewModel.updateUseDynamicColors(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    appTheme: AppTheme,
    appAccent: AppAccent,
    onUpdateAccent: (AppAccent) -> Unit,
    useDynamicColors: Boolean,
    onUpdateTheme: (AppTheme) -> Unit,
    onUpdateDynamicColors: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.theme_settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.base_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            val baseThemes = listOf(
                AppTheme.SYSTEM to R.string.system_default,
                AppTheme.LIGHT to R.string.light,
                AppTheme.DARK to R.string.dark
            )

            baseThemes.forEach { (theme, labelRes) ->
                item {
                    val label = stringResource(labelRes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateTheme(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appTheme == theme,
                            onClick = { onUpdateTheme(theme) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Catppuccin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val catppuccinThemes = listOf(
                        AppTheme.CATPPUCCIN_LATTE to ("Latte" to Catppuccin.Latte.Base),
                        AppTheme.CATPPUCCIN_FRAPPE to ("Frappe" to Catppuccin.Frappe.Base),
                        AppTheme.CATPPUCCIN_MACCHIATO to ("Macchiato" to Catppuccin.Macchiato.Base),
                        AppTheme.CATPPUCCIN_MOCHA to ("Mocha" to Catppuccin.Mocha.Base)
                    )

                    catppuccinThemes.forEach { (theme, info) ->
                        val (label, color) = info
                        FilterChip(
                            selected = appTheme == theme,
                            onClick = { onUpdateTheme(theme) },
                            label = { Text(label) },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(16.dp),
                                    shape = CircleShape,
                                    color = color,
                                    content = {}
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }


            if (appTheme == AppTheme.CATPPUCCIN_LATTE || appTheme == AppTheme.CATPPUCCIN_FRAPPE || appTheme == AppTheme.CATPPUCCIN_MACCHIATO || appTheme == AppTheme.CATPPUCCIN_MOCHA) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isDark = appTheme != AppTheme.CATPPUCCIN_LATTE
                        val palette = if (isDark) Catppuccin.Mocha else Catppuccin.Latte

                        val accents = listOf(
                            AppAccent.ROSEWATER to ("Rosewater" to palette.Rosewater),
                            AppAccent.FLAMINGO to ("Flamingo" to palette.Flamingo),
                            AppAccent.PINK to ("Pink" to palette.Pink),
                            AppAccent.MAUVE to ("Mauve" to palette.Mauve),
                            AppAccent.RED to ("Red" to palette.Red),
                            AppAccent.MAROON to ("Maroon" to palette.Maroon),
                            AppAccent.PEACH to ("Peach" to palette.Peach),
                            AppAccent.YELLOW to ("Yellow" to palette.Yellow),
                            AppAccent.GREEN to ("Green" to palette.Green),
                            AppAccent.TEAL to ("Teal" to palette.Teal),
                            AppAccent.SKY to ("Sky" to palette.Sky),
                            AppAccent.SAPPHIRE to ("Sapphire" to palette.Sapphire),
                            AppAccent.BLUE to ("Blue" to palette.Blue),
                            AppAccent.LAVENDER to ("Lavender" to palette.Lavender)
                        )

                        accents.forEach { (accent, info) ->
                            val (label, color) = info
                            FilterChip(
                                selected = appAccent == accent,
                                onClick = { onUpdateAccent(accent) },
                                label = { Text(label) },
                                leadingIcon = {
                                    Surface(
                                        modifier = Modifier.size(16.dp),
                                        shape = CircleShape,
                                        color = color,
                                        content = {}
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.dynamic_colors),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateDynamicColors(!useDynamicColors) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.use_material_you),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.extracts_colors_wallpaper),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = { onUpdateDynamicColors(it) }
                        )
                    }
                }
            }
        }
    }
}
