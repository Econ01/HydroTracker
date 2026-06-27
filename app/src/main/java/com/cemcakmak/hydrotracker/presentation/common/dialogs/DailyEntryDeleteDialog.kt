package com.cemcakmak.hydrotracker.presentation.common.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.UserProfile

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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                stringResource(
                    if (entry.isExternalEntry()) {
                        R.string.home_dialog_external_entry_title
                    } else {
                        R.string.home_dialog_edit_entry_title
                    }
                )
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.home_dialog_delete_message,
                    containerLabel,
                    entry.getFormattedAmount(context, userProfile.volumeUnit)
                )
            )
        },
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
                                text = stringResource(R.string.action_cancel),
                                maxLines = 1,
                                softWrap = false
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
