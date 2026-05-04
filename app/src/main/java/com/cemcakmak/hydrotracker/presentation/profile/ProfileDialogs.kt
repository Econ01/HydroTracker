package com.cemcakmak.hydrotracker.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import java.util.Locale

/**
 * Profile Edit Bottom Sheets
 * Material 3 ModalBottomSheet components for editing profile information
 */

/**
 * Bottom Sheet for editing daily water goal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditBottomSheet(
    showBottomSheet: Boolean,
    currentGoal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    var sliderValue by remember(currentGoal) { mutableFloatStateOf(currentGoal.toFloat() / 1000f) }
    var goalText by remember(currentGoal) { mutableStateOf(String.format(Locale.getDefault(), "%.2f", currentGoal / 1000)) }
    var isError by remember { mutableStateOf(false) }

    // Update text when slider changes
    LaunchedEffect(sliderValue) {
        goalText = String.format(Locale.getDefault(), "%.2f", sliderValue)
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Edit Daily Goal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Set your daily water intake goal. This will be used to track your progress and calculate reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Haptics
                val haptics = LocalHapticFeedback.current

                // Track last indices so we only fire once per step
                val minL = 1.0f
                val smallStep = 0.1f         // frequent tick every 0.1 L
                val majorStep = 0.50f         // stronger tick every 0.5 L

                var lastSmallIdx by remember { mutableIntStateOf(((sliderValue - minL) / smallStep).toInt()) }
                var lastMajorIdx by remember { mutableIntStateOf(((sliderValue - minL) / majorStep).toInt()) }

                // Slider Control
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Use slider to adjust: ${String.format(Locale.getDefault(), "%.2f", sliderValue)} L",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Slider(
                        value = sliderValue.coerceIn(1.0f, 5.0f),
                        onValueChange = { newValue ->
                            // compute step indices
                            val smallIdx = ((newValue - minL) / smallStep).toInt()
                            val majorIdx = ((newValue - minL) / majorStep).toInt()

                            // fire a stronger tick at major marks, else a light tick at each step
                            if (majorIdx != lastMajorIdx) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                lastMajorIdx = majorIdx
                            } else if (smallIdx != lastSmallIdx) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            }
                            lastSmallIdx = smallIdx

                            sliderValue = newValue
                            isError = false
                        },
                        onValueChangeFinished = {
                            // nice end-of-gesture pulse (optional)
                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                        valueRange = 1.0f..5.0f,
                        steps = 39, // 0.1L increments: (5.0 - 1.0) / 0.25 - 1
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1.0L",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "5.0L",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Text Input (Alternative)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Or enter manually:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        shape = RoundedCornerShape(12.dp),
                        value = goalText,
                        onValueChange = { newText ->
                            goalText = newText
                            isError = false
                            // Update slider when text changes (only if within slider range)
                            newText.toDoubleOrNull()?.let { value ->
                                if (value in 1.0..5.0) {
                                    sliderValue = value.toFloat()
                                }
                            }
                        },
                        label = { Text("Daily Goal (Liters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("Please enter a valid amount (0.5-10 L)") }
                        } else {
                            { Text("Recommended: 1.5 - 4.0 liters per day") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onDismiss },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp), text = "Cancel"
                        )
                    }

                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            val goalLiters = sliderValue.toDouble()
                            if (goalLiters in 0.5..10.0) {
                                onConfirm(goalLiters * 1000) // Convert to ml
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp), text = "Save"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Bottom Sheet for selecting activity level
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLevelBottomSheet(
    showBottomSheet: Boolean,
    currentLevel: ActivityLevel,
    onDismiss: () -> Unit,
    onConfirm: (ActivityLevel) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Activity Level",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Your activity level affects your daily water needs. Changing this will automatically update your daily goal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val haptics = LocalHapticFeedback.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActivityLevel.entries.forEach { level ->
                        Card(
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onConfirm(level) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (level == currentLevel) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = level.getDisplayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = level.getDescription(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Bottom Sheet for editing wake/sleep schedule
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditBottomSheet(
    showBottomSheet: Boolean,
    currentWakeUpTime: String,
    currentSleepTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    
    // Parse current times
    val wakeUpParts = currentWakeUpTime.split(":")
    val sleepParts = currentSleepTime.split(":")
    
    var wakeUpHour by remember(currentWakeUpTime) { mutableIntStateOf(wakeUpParts[0].toIntOrNull() ?: 7) }
    var wakeUpMinute by remember(currentWakeUpTime) { mutableIntStateOf(wakeUpParts[1].toIntOrNull() ?: 0) }
    var sleepHour by remember(currentSleepTime) { mutableIntStateOf(sleepParts[0].toIntOrNull() ?: 23) }
    var sleepMinute by remember(currentSleepTime) { mutableIntStateOf(sleepParts[1].toIntOrNull() ?: 0) }
    
    var showWakeUpTimePicker by remember { mutableStateOf(false) }
    var showSleepTimePicker by remember { mutableStateOf(false) }

    val wakeUpTimeState = rememberTimePickerState(
        initialHour = wakeUpHour,
        initialMinute = wakeUpMinute,
        is24Hour = true
    )
    
    val sleepTimeState = rememberTimePickerState(
        initialHour = sleepHour,
        initialMinute = sleepMinute,
        is24Hour = true
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Edit Schedule",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Set your daily schedule to get reminders only during your active hours. This will automatically adjust your reminder frequency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Wake Up Time Picker
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Wake Up Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Card(
                            onClick = { showWakeUpTimePicker = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", wakeUpHour, wakeUpMinute),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sleep Time Picker
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sleep Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Card(
                            onClick = { showSleepTimePicker = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onDismiss },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            val wakeUpFormatted = String.format(Locale.getDefault(), "%02d:%02d", wakeUpHour, wakeUpMinute)
                            val sleepFormatted = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute)
                            onConfirm(wakeUpFormatted, sleepFormatted)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Wake Up Time Picker Dialog
    if (showWakeUpTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showWakeUpTimePicker = false },
            onConfirm = {
                wakeUpHour = wakeUpTimeState.hour
                wakeUpMinute = wakeUpTimeState.minute
                showWakeUpTimePicker = false
            }
        ) {
            TimePicker(state = wakeUpTimeState)
        }
    }

    // Sleep Time Picker Dialog
    if (showSleepTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showSleepTimePicker = false },
            onConfirm = {
                sleepHour = sleepTimeState.hour
                sleepMinute = sleepTimeState.minute
                showSleepTimePicker = false
            }
        ) {
            TimePicker(state = sleepTimeState)
        }
    }
}

/**
 * Time Picker Dialog Component
 */
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        text = { content() }
    )
}

/**
 * Bottom Sheet for editing gender
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderEditBottomSheet(
    showBottomSheet: Boolean,
    currentGender: Gender,
    onDismiss: () -> Unit,
    onConfirm: (Gender) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Select Gender",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Gender helps us calculate more accurate hydration recommendations based on physiological differences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val haptics = LocalHapticFeedback.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Gender.entries.forEach { gender ->
                        Card(
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onConfirm(gender) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (gender == currentGender) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = gender.getDisplayName(),
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (gender == currentGender) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Bottom Sheet for editing age group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeGroupEditBottomSheet(
    showBottomSheet: Boolean,
    currentAgeGroup: AgeGroup,
    onDismiss: () -> Unit,
    onConfirm: (AgeGroup) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Select Age Group",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Age affects your daily water needs. Different age groups have varying hydration requirements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val haptics = LocalHapticFeedback.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AgeGroup.entries.forEach { ageGroup ->
                        Card(
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onConfirm(ageGroup) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (ageGroup == currentAgeGroup) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ageGroup.getDisplayName(),
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (ageGroup == currentAgeGroup) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Bottom Sheet for editing weight
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEditBottomSheet(
    showBottomSheet: Boolean,
    currentWeight: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    var weightText by remember(currentWeight) { mutableStateOf(currentWeight?.toString() ?: "") }
    var isWeightError by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter Weight",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Adding your weight helps us provide more accurate hydration recommendations. This information is optional and stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    shape = RoundedCornerShape(12.dp),
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        isWeightError = false
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isWeightError,
                    supportingText = if (isWeightError) {
                        { Text("Please enter a valid weight (30-300 kg)") }
                    } else {
                        { Text("Leave empty if you prefer not to share") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onDismiss },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp), text = "Cancel"
                        )
                    }

                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            val weight = if (weightText.isBlank()) {
                                null
                            } else {
                                weightText.toDoubleOrNull()
                            }

                            if (weight != null && (weight !in 30.0..300.0)) {
                                isWeightError = true
                            } else {
                                onConfirm(weight)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp), text = "Save"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}