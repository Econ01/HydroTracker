package com.cemcakmak.hydrotracker.presentation.common.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

/**
 * Confirmation dialogue shown before deleting a water intake entry.
 *
 * @param entry The entry that is about to be deleted.
 * @param userProfile Profile used for volume formatting.
 * @param onConfirm Called when the user confirms deletion.
 * @param onDismiss Called when the user cancels or dismisses the dialogue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryDeleteDialog(
    entry: WaterIntakeEntry,
    userProfile: UserProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preset = remember(entry.containerType) {
        ContainerPreset.getDefaultPresets().firstOrNull { it.name == entry.containerType }
    }
    val containerLabel = when {
        preset?.labelResId != 0 && preset?.labelResId != null -> stringResource(preset.labelResId)
        entry.containerType == "Custom" -> stringResource(R.string.home_option_custom)
        else -> entry.containerType
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLargeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(
                            if (entry.isExternalEntry()) {
                                R.string.home_dialog_external_entry_title
                            } else {
                                R.string.home_dialog_edit_entry_title
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(
                        R.string.home_dialog_delete_message,
                        containerLabel,
                        entry.getFormattedAmount(context, userProfile.volumeUnit)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Delete Entry Dialog")
@Composable
private fun DailyEntryDeleteDialogPreview() {
    val previewUser = UserProfile(
        name = "Preview User",
        gender = Gender.FEMALE,
        ageGroup = AgeGroup.YOUNG_ADULT_18_30,
        activityLevel = ActivityLevel.LIGHT,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2000.0,
        reminderInterval = 60,
        volumeUnit = VolumeUnit.MILLILITRES
    )

    HydroTrackerTheme {
        DailyEntryDeleteDialog(
            entry = WaterIntakeEntry(
                id = 1,
                amount = 500.0,
                timestamp = System.currentTimeMillis(),
                date = "2026-06-21",
                containerType = "Bottle",
                containerVolume = 500.0,
                beverageType = BeverageType.WATER.name
            ),
            userProfile = previewUser,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
