/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectDataScreen(
    waterIntakeRepository: WaterIntakeRepository?,
    userProfile: UserProfile? = null,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    var allEntries by remember { mutableStateOf<List<WaterIntakeEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Load entries when screen opens or refreshTrigger changes
    LaunchedEffect(waterIntakeRepository, refreshTrigger) {
        if (waterIntakeRepository != null) {
            try {
                isLoading = true

                // Collect both flows and combine them
                val visibleFlow = waterIntakeRepository.getAllEntries()
                val hiddenFlow = waterIntakeRepository.getHiddenEntries()

                // Use combine to merge both flows
                kotlinx.coroutines.flow.combine(visibleFlow, hiddenFlow) { visible, hidden ->
                    visible to hidden
                }.collect { (visible, hidden) ->
                    allEntries = visible + hidden
                    isLoading = false
                }
            } catch (_: Exception) {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hc_data_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (allEntries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.hc_no_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.hc_no_data_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allEntries.sortedByDescending { it.timestamp }) { entry ->
                        HealthConnectDataItem(
                            entry = entry,
                            waterIntakeRepository = waterIntakeRepository,
                            userProfile = userProfile,
                            themePreferences = themePreferences,
                            onEntryChanged = {
                                // Trigger refresh
                                refreshTrigger++
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthConnectDataItem(
    entry: WaterIntakeEntry,
    waterIntakeRepository: WaterIntakeRepository?,
    userProfile: UserProfile?,
    themePreferences: ThemePreferences,
    onEntryChanged: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES

    val isExternal = entry.isExternalEntry()
    val isHidden = entry.isHidden

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else if (isExternal) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with amount and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = entry.getFormattedEffectiveAmount(context, volumeUnit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isHidden) {
                        AssistChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.hc_badge_hidden), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (isExternal) {
                        AssistChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.hc_badge_external), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date and time formatted according to user preferences.
            // The entry timestamp is still read from the database unchanged.
            val formattedDateTime = remember(entry.timestamp, themePreferences) {
                DateTimeFormatters.formatDateTime(
                    context,
                    java.time.Instant.ofEpochMilli(entry.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime(),
                    themePreferences.timeFormat,
                    themePreferences.dateFormat
                )
            }
            Text(
                text = formattedDateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Container info with beverage type details
            if (entry.containerType.isNotEmpty()) {
                val beverageType = entry.getBeverageType()
                val rawAmountText = entry.getFormattedAmount(context, volumeUnit)
                val effectiveAmountText = entry.getFormattedEffectiveAmount(context, volumeUnit)
                val containerVolumeText = VolumeUnitConverter.format(context, entry.containerVolume, volumeUnit)

                Text(
                    text = stringResource(R.string.hc_container_line, entry.containerType, containerVolumeText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show beverage type and effective amount info
                if (beverageType.hydrationMultiplier != 1.0) {
                    Text(
                        text = stringResource(R.string.hc_beverage_detail_eff, stringResource(beverageType.labelResId), rawAmountText, effectiveAmountText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.hc_beverage_detail, stringResource(beverageType.labelResId), effectiveAmountText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Note
            if (!entry.note.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.hc_note_label, entry.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Health Connect record ID
            if (!entry.healthConnectRecordId.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.hc_record_id, entry.healthConnectRecordId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action button for hidden external entries
            if (isHidden && isExternal && waterIntakeRepository != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                waterIntakeRepository.unhideWaterIntake(entry)
                                onEntryChanged()
                            } catch (_: Exception) {
                                // Handle error
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.hc_unhide_entry))
                }
            }
        }
    }
}