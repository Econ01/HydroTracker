package com.cemcakmak.hydrotracker.presentation.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import androidx.compose.ui.platform.LocalContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleStep(
    wakeUpTime: String,
    sleepTime: String,
    onWakeUpTimeChanged: (String) -> Unit,
    onSleepTimeChanged: (String) -> Unit,
    title: String,
    description: String,
    themePreferences: ThemePreferences = ThemePreferences()
) {
    var showWakeUpPicker by remember { mutableStateOf(false) }
    var showSleepPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title & Subtitle
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Wake-Up Card
        TimeSelectionCard(
            label = stringResource(R.string.onboarding_wake_label),
            time = wakeUpTime,
            icon = Icons.Default.WbSunny,
            themePreferences = themePreferences
        ) { showWakeUpPicker = true }

        // Sleep Card
        TimeSelectionCard(
            label = stringResource(R.string.onboarding_sleep_label),
            time = sleepTime,
            icon = Icons.Default.Bedtime,
            themePreferences = themePreferences
        ) { showSleepPicker = true }

        // Preview Timeline
        DayDurationPreview(wakeUpTime, sleepTime)
    }

    // Time Pickers
    if (showWakeUpPicker) {
        ExpressiveTimePickerDialog(
            title = stringResource(R.string.onboarding_wake_picker_title),
            subtitle = stringResource(R.string.onboarding_wake_picker_subtitle),
            icon = Icons.Default.WbSunny,
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showWakeUpPicker = false },
            onConfirm = { hour, minute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                onWakeUpTimeChanged(timeString)
                showWakeUpPicker = false
            },
            initialTime = wakeUpTime
        )
    }

    if (showSleepPicker) {
        ExpressiveTimePickerDialog(
            title = stringResource(R.string.onboarding_sleep_picker_title),
            subtitle = stringResource(R.string.onboarding_sleep_picker_subtitle),
            icon = Icons.Default.Bedtime,
            accentColor = MaterialTheme.colorScheme.secondary,
            onDismiss = { showSleepPicker = false },
            onConfirm = { hour, minute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                onSleepTimeChanged(timeString)
                showSleepPicker = false
            },
            initialTime = sleepTime
        )
    }
}

@Preview
@Composable
fun ScheduleStepPreview() {
    ScheduleStep(
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        onWakeUpTimeChanged = {},
        onSleepTimeChanged = {},
        title = "Set Your Schedule",
        description = "Tell us when you typically wake up and go to sleep."
    )
}



@Composable
fun TimeSelectionCard(
    label: String,
    time: String,
    icon: ImageVector,
    themePreferences: ThemePreferences = ThemePreferences(),
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
            .clip(MaterialTheme.shapes.extraLargeIncreased),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Analogue clock
            AnalogClock(time = time, size = 50.dp)

            // Text Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DateTimeFormatters.formatTimeString(context, time, themePreferences.timeFormat),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icon on the right
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview
@Composable
fun TimeSelectionCardPreview() {
    TimeSelectionCard(
        label = "Wake-Up Time",
        time = "07:00",
        icon = Icons.Default.WbSunny,
        onClick = {}
    )
}


@Composable
fun AnalogClock(time: String, size: Dp) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val localTime = try {
        LocalTime.parse(time, formatter)
    } catch (_: Exception) {
        LocalTime.of(7, 0)
    }

    Canvas(modifier = Modifier.size(size)) {
        val radius = size.toPx() / 2
        val center = Offset(radius, radius)

        // Clock border
        drawCircle(
            color = surfaceVariantColor,
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )

        // Hour hand
        val hourRotation = (localTime.hour % 12) * 30f + (localTime.minute / 2f)
        val hourAngle = Math.toRadians(hourRotation.toDouble())
        val hourLength = radius * 0.5f
        drawLine(
            color = surfaceColor,
            start = center,
            end = Offset(
                center.x + hourLength * sin(hourAngle).toFloat(),
                center.y - hourLength * cos(hourAngle).toFloat()
            ),
            strokeWidth = 6f
        )

        // Minute hand
        val minuteRotation = localTime.minute * 6f
        val minuteAngle = Math.toRadians(minuteRotation.toDouble())
        val minuteLength = radius * 0.8f
        drawLine(
            color = primaryColor,
            start = center,
            end = Offset(
                center.x + minuteLength * sin(minuteAngle).toFloat(),
                center.y - minuteLength * cos(minuteAngle).toFloat()
            ),
            strokeWidth = 4f
        )
    }
}

@Preview
@Composable
fun AnalogClockPreview() {
    AnalogClock(time = "10:30", size = 100.dp)
}


@Composable
private fun DayDurationPreview(wakeUpTime: String, sleepTime: String) {
    val awakeHours = calculateAwakeHours(wakeUpTime, sleepTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { (awakeHours / 24.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(
                R.string.onboarding_awake_duration,
                awakeHours.toInt(),
                ((awakeHours % 1) * 60).toInt()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
fun DayDurationPreviewPreview() {
    DayDurationPreview(wakeUpTime = "07:00", sleepTime = "23:00")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveTimePickerDialog(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialTime: String
) {
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = accentColor,
                        timeSelectorSelectedContainerColor = accentColor,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.time_picker_set), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ExpressiveTimePickerDialogPreview() {
    ExpressiveTimePickerDialog(
        title = "Wake Up Time",
        subtitle = "Choose your wake-up time",
        icon = Icons.Default.WbSunny,
        accentColor = MaterialTheme.colorScheme.primary,
        onDismiss = {},
        onConfirm = { _, _ -> },
        initialTime = "07:00"
    )
}

private fun calculateAwakeHours(wakeUpTime: String, sleepTime: String): Double {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val wakeUp = LocalTime.parse(wakeUpTime, formatter)
        val sleep = LocalTime.parse(sleepTime, formatter)

        val awakeMinutes = if (sleep.isAfter(wakeUp)) {
            sleep.toSecondOfDay() - wakeUp.toSecondOfDay()
        } else {
            (24 * 3600) - wakeUp.toSecondOfDay() + sleep.toSecondOfDay()
        }

        awakeMinutes / 3600.0
    } catch (_: Exception) {
        16.0 // fallback
    }
}
