/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.backup.DataBackupManager
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.CustomBeverageRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    customBeverageRepository: CustomBeverageRepository,
    userRepository: UserRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var isBusy by remember { mutableStateOf(false) }

    var showDeleteBeforeDateDialog by remember { mutableStateOf(false) }
    var showDeleteLocalDialog by remember { mutableStateOf(false) }
    var showDeleteHealthConnectDialog by remember { mutableStateOf(false) }
    var showDeleteEverythingDialog by remember { mutableStateOf(false) }

    val exportSuccessTemplate = stringResource(R.string.data_export_success)
    val exportFailedTemplate = stringResource(R.string.data_export_failed)
    val importSuccessTemplate = stringResource(R.string.data_import_success)
    val importFailedTemplate = stringResource(R.string.data_import_failed)
    val deleteSuccessTemplate = stringResource(R.string.data_delete_success)
    val deleteFailedTemplate = stringResource(R.string.data_delete_failed)

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                isBusy = true
                val result = DataBackupManager.export(
                    context, waterIntakeRepository, it, DataBackupManager.BackupFormat.CSV
                )
                isBusy = false
                result.fold(
                    onSuccess = { count ->
                        Toast.makeText(
                            context,
                            String.format(exportSuccessTemplate, count),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            String.format(exportFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isBusy = true
                val result = DataBackupManager.export(
                    context, waterIntakeRepository, it, DataBackupManager.BackupFormat.JSON
                )
                isBusy = false
                result.fold(
                    onSuccess = { count ->
                        Toast.makeText(
                            context,
                            String.format(exportSuccessTemplate, count),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            String.format(exportFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isBusy = true
                val result = DataBackupManager.import(context, waterIntakeRepository, it)
                isBusy = false
                result.fold(
                    onSuccess = { importResult ->
                        Toast.makeText(
                            context,
                            String.format(
                                importSuccessTemplate,
                                importResult.imported,
                                importResult.skipped
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            String.format(importFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.screen_data_management_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        // Export and Import
        SettingsSectionHeader(stringResource(R.string.data_management_backup_header))

        Column {
            SettingsGroupCard(
                index = 0,
                size = 3,
                onClick = if (!isBusy) {{
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    importLauncher.launch(arrayOf("text/csv", "application/json"))
                }} else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.upload_file_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_import_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.data_import_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_right_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsGroupCard(
                index = 1,
                size = 3,
                onClick = if (!isBusy) {{
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    exportCsvLauncher.launch(defaultExportFileName("csv"))
                }} else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.csv_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_export_csv_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_right_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsGroupCard(
                index = 2,
                size = 3,
                onClick = if (!isBusy) {{
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    exportJsonLauncher.launch(defaultExportFileName("json"))
                }} else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.file_json_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_export_json_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_right_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.data_export_scope_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }

        // Destructive actions
        SettingsSectionHeader(stringResource(R.string.data_management_delete_header))

        Column {
            DeleteActionCard(
                index = 0,
                title = stringResource(R.string.data_delete_before_date_title),
                description = stringResource(R.string.data_delete_before_date_desc),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDeleteBeforeDateDialog = true
                },
                enabled = !isBusy
            )
            DeleteActionCard(
                index = 1,
                title = stringResource(R.string.data_delete_local_title),
                description = stringResource(R.string.data_delete_local_desc),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDeleteLocalDialog = true
                },
                enabled = !isBusy
            )
            DeleteActionCard(
                index = 2,
                title = stringResource(R.string.data_delete_health_connect_title),
                description = stringResource(R.string.data_delete_health_connect_desc),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDeleteHealthConnectDialog = true
                },
                enabled = !isBusy
            )
            DeleteActionCard(
                index = 3,
                title = stringResource(R.string.data_delete_everything_title),
                description = stringResource(R.string.data_delete_everything_desc),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDeleteEverythingDialog = true
                },
                enabled = !isBusy
            )
        }
    }

    if (showDeleteBeforeDateDialog) {
        val healthConnectAvailable = remember { HealthConnectManager.isAvailable(context) }
        DeleteBeforeDateDialog(
            onDismiss = { showDeleteBeforeDateDialog = false },
            countEntriesBefore = { date -> waterIntakeRepository.countEntriesBefore(date) },
            healthConnectAvailable = healthConnectAvailable,
            onConfirm = { date, includeHealthConnect ->
                showDeleteBeforeDateDialog = false
                scope.launch {
                    isBusy = true
                    val localResult = waterIntakeRepository.deleteEntriesBefore(date)
                    val hcResult = if (includeHealthConnect && HealthConnectManager.isAvailable(context)) {
                        waterIntakeRepository.deleteHealthConnectEntriesBefore(context, date)
                    } else {
                        Result.success(0)
                    }
                    isBusy = false

                    if (localResult.isSuccess && hcResult.isSuccess) {
                        Toast.makeText(
                            context,
                            deleteSuccessTemplate,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val error = localResult.exceptionOrNull() ?: hcResult.exceptionOrNull()
                        Toast.makeText(
                            context,
                            String.format(deleteFailedTemplate, error?.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    if (showDeleteLocalDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.data_delete_local_title),
            message = stringResource(R.string.data_delete_local_message),
            onDismiss = { showDeleteLocalDialog = false },
            onConfirm = {
                showDeleteLocalDialog = false
                scope.launch {
                    isBusy = true
                    try {
                        waterIntakeRepository.clearAllData().getOrThrow()
                        containerPresetRepository.resetToDefaults()
                        customBeverageRepository.deleteAll()
                        userRepository.clearUserProfile()
                        Toast.makeText(
                            context,
                            deleteSuccessTemplate,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            String.format(deleteFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isBusy = false
                    }
                }
            }
        )
    }

    if (showDeleteHealthConnectDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.data_delete_health_connect_title),
            message = stringResource(R.string.data_delete_health_connect_message),
            onDismiss = { showDeleteHealthConnectDialog = false },
            onConfirm = {
                showDeleteHealthConnectDialog = false
                scope.launch {
                    isBusy = true
                    try {
                        waterIntakeRepository.deleteAllHealthConnectData(context).getOrThrow()
                        Toast.makeText(
                            context,
                            deleteSuccessTemplate,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            String.format(deleteFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isBusy = false
                    }
                }
            }
        )
    }

    if (showDeleteEverythingDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.data_delete_everything_title),
            message = stringResource(R.string.data_delete_everything_message),
            onDismiss = { showDeleteEverythingDialog = false },
            onConfirm = {
                showDeleteEverythingDialog = false
                scope.launch {
                    isBusy = true
                    try {
                        waterIntakeRepository.deleteAllHealthConnectData(context).getOrThrow()
                        waterIntakeRepository.clearAllData().getOrThrow()
                        containerPresetRepository.resetToDefaults()
                        customBeverageRepository.deleteAll()
                        userRepository.clearUserProfile()
                        Toast.makeText(
                            context,
                            deleteSuccessTemplate,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            String.format(deleteFailedTemplate, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isBusy = false
                    }
                }
            }
        )
    }
}

@Composable
private fun DeleteBeforeDateDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: LocalDate, includeHealthConnect: Boolean) -> Unit,
    countEntriesBefore: suspend (LocalDate) -> Int,
    healthConnectAvailable: Boolean
) {
    var chosenDate by remember { mutableStateOf<LocalDate?>(null) }
    var chosenCount by remember { mutableIntStateOf(0) }

    val date = chosenDate
    if (date == null) {
        DeleteBeforeDatePickerStep(
            onDismiss = onDismiss,
            onContinue = { picked, count ->
                chosenDate = picked
                chosenCount = count
            },
            countEntriesBefore = countEntriesBefore
        )
    } else {
        DeleteBeforeDateConfirmStep(
            date = date,
            affectedCount = chosenCount,
            healthConnectAvailable = healthConnectAvailable,
            onDismiss = onDismiss,
            onConfirm = { includeHealthConnect -> onConfirm(date, includeHealthConnect) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteBeforeDatePickerStep(
    onDismiss: () -> Unit,
    onContinue: (date: LocalDate, affectedCount: Int) -> Unit,
    countEntriesBefore: suspend (LocalDate) -> Int
) {
    val haptics = LocalHapticFeedback.current

    val defaultDateMillis = remember {
        LocalDate.now()
            .minusDays(30)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = defaultDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    // Live count of local entries affected by the selected cutoff date.
    var affectedCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis
        affectedCount = if (millis != null) {
            try {
                countEntriesBefore(
                    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                )
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onContinue(date, affectedCount ?: 0)
                }
            ) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
}


@Composable
private fun DeleteBeforeDateConfirmStep(
    date: LocalDate,
    affectedCount: Int,
    healthConnectAvailable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (includeHealthConnect: Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var includeHealthConnect by remember { mutableStateOf(healthConnectAvailable) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy") }


    ConfirmDeleteDialog(
        title = stringResource(
            R.string.data_delete_before_date_model_title,
            date.format(dateFormatter)
        ),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = if (affectedCount == 0) {
                        stringResource(R.string.data_delete_no_entries_affected)
                    } else {
                        pluralStringResource(
                            R.plurals.data_delete_entries_affected,
                            affectedCount,
                            affectedCount, date.format(dateFormatter)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (healthConnectAvailable) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.data_delete_health_connect_include),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = includeHealthConnect,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                includeHealthConnect = it
                            },
                            thumbContent = if (includeHealthConnect) {
                                {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.check_filled),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        },
        confirmEnabled = affectedCount > 0 || includeHealthConnect,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(includeHealthConnect) }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmDeleteDialog(
        title = title,
        text = { Text(message) },
        confirmEnabled = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    text: @Composable () -> Unit,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title) },
        text = text,
        confirmButton = {
            val haptics = LocalHapticFeedback.current
            val cancelInteractionSource = remember { MutableInteractionSource() }
            val confirmDeleteInteractionSource = remember { MutableInteractionSource() }

            LaunchedEffect(cancelInteractionSource) {
                cancelInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        else -> {  }
                    }
                }
            }

            LaunchedEffect(confirmDeleteInteractionSource) {
                confirmDeleteInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        else -> {  }
                    }
                }
            }

            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                overflowIndicator = {}
            ) {
                val scope = this
                customItem(
                    buttonGroupContent = {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                            },
                            shapes = ButtonDefaults.shapes(),
                            interactionSource = cancelInteractionSource,
                            modifier = with(scope) {
                                Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .animateWidth(interactionSource = cancelInteractionSource)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel)
                            )
                        }
                    },
                    menuContent = {}
                )

                customItem(
                    buttonGroupContent = {
                        Button(
                            onClick = {
                                onConfirm()
                            },
                            enabled = confirmEnabled,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shapes = ButtonDefaults.shapes(),
                            interactionSource = confirmDeleteInteractionSource,
                            modifier = with(scope) {
                                Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .animateWidth(interactionSource = confirmDeleteInteractionSource)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.action_delete),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    },
                    menuContent = {}
                )
            }
        }
    )
}

@Composable
private fun DeleteActionCard(
    index: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    SettingsGroupCard(
        index = index,
        size = 4,
        onClick = if (enabled) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.delete_fill),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!enabled) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_right_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun defaultExportFileName(extension: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val date = LocalDate.now().format(formatter)
    return "hydrotracker_export_${date}.$extension"
}

@Preview(showBackground = true)
@Composable
fun DataManagementScreenPreview() {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val waterIntakeRepository = remember {
        com.cemcakmak.hydrotracker.data.database.DatabaseInitializer.getWaterIntakeRepository(
            context, userRepository
        )
    }
    val containerPresetRepository = remember {
        com.cemcakmak.hydrotracker.data.database.DatabaseInitializer.getContainerPresetRepository(
            context
        )
    }
    val customBeverageRepository = remember {
        com.cemcakmak.hydrotracker.data.database.DatabaseInitializer.getCustomBeverageRepository(
            context
        )
    }

    HydroTrackerTheme {
        DataManagementScreen(
            waterIntakeRepository = waterIntakeRepository,
            containerPresetRepository = containerPresetRepository,
            customBeverageRepository = customBeverageRepository,
            userRepository = userRepository,
            themePreferences = ThemePreferences(),
            onNavigateBack = {}
        )
    }
}