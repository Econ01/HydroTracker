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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectDataScreen(
    waterIntakeRepository: WaterIntakeRepository?,
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
                        text = "Health Connect Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                        text = "No Water Intake Data",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "No water intake entries found in the database.",
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
    onEntryChanged: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Use the localized date-time formatting from the entry itself

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
                        text = "${entry.getEffectiveHydrationAmount().toInt()} ml",
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
                            label = { Text("Hidden", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (isExternal) {
                        AssistChip(
                            onClick = { },
                            label = { Text("External", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date and time (fixed ISO 24-hour format for Health Connect consistency)
            val isoFormatter = remember {
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            }
            val isoDateTime = remember(entry.timestamp) {
                java.time.Instant.ofEpochMilli(entry.timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(isoFormatter)
            }
            Text(
                text = isoDateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Container info with beverage type details
            if (entry.containerType.isNotEmpty()) {
                val beverageType = entry.getBeverageType()
                val rawAmount = entry.amount.toInt()
                val effectiveAmount = entry.getEffectiveHydrationAmount().toInt()

                Text(
                    text = "Container: ${entry.containerType} (${entry.containerVolume.toInt()} ml)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show beverage type and effective amount info
                if (beverageType.hydrationMultiplier != 1.0) {
                    Text(
                        text = "${beverageType.displayName} • ${rawAmount}ml raw → ${effectiveAmount}ml effective",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${beverageType.displayName} • ${rawAmount}ml effective",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Note
            if (!entry.note.isNullOrEmpty()) {
                Text(
                    text = "Note: ${entry.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Health Connect record ID
            if (!entry.healthConnectRecordId.isNullOrEmpty()) {
                Text(
                    text = "Health Connect ID: ${entry.healthConnectRecordId}",
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
                    Text("Unhide Entry")
                }
            }
        }
    }
}