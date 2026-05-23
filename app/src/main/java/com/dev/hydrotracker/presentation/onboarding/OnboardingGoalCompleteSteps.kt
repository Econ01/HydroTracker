// OnboardingGoalCompleteSteps.kt
package com.dev.hydrotracker.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dev.hydrotracker.data.models.*
import com.dev.hydrotracker.utils.WaterCalculator

@Composable
fun GoalStep(
    userProfile: UserProfile,
    title: String,
    description: String
) {
    OnboardingStepLayout(
        title = title,
        description = description
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            ExpressiveHeaderStrip()

            GoalInfoCard(
                icon = Icons.Rounded.WaterDrop,
                title = "Daily Water Goal",
                value = WaterCalculator.formatWaterAmount(userProfile.dailyWaterGoal),
                accent = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surface,
                iconContainer = MaterialTheme.colorScheme.primaryContainer
            )

            GoalInfoCard(
                icon = Icons.Rounded.AccessTime,
                title = "Reminder Interval",
                value = "${userProfile.reminderInterval} minutes",
                accent = MaterialTheme.colorScheme.tertiary,
                container = MaterialTheme.colorScheme.surface,
                iconContainer = MaterialTheme.colorScheme.tertiaryContainer
            )

            GoalInfoCard(
                icon = Icons.Rounded.FitnessCenter,
                title = "Activity Level",
                value = userProfile.activityLevel.getDisplayName(),
                accent = MaterialTheme.colorScheme.secondary,
                container = MaterialTheme.colorScheme.surface,
                iconContainer = MaterialTheme.colorScheme.secondaryContainer
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "How we personalized this",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your goal considers your profile and schedule. These factors influence total intake and notification cadence.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                    Spacer(Modifier.height(12.dp))
                    ReasonChipsRow(userProfile = userProfile)
                }
            }
        }
    }
}

@Composable
private fun ExpressiveHeaderStrip() {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        c.primary.copy(alpha = 0.45f),
                        c.secondary.copy(alpha = 0.45f),
                        c.tertiary.copy(alpha = 0.45f)
                    )
                )
            )
            .semantics { contentDescription = "expressive header accent" }
    )
}

@Composable
private fun ReasonChipsRow(userProfile: UserProfile) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text(userProfile.gender.displayName()) },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = c.onSurfaceVariant,
                containerColor = c.surface
            ),
            border = BorderStroke(1.dp, c.outlineVariant)
        )
        AssistChip(
            onClick = {},
            label = { Text(userProfile.ageGroup.displayName()) },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = c.onSurfaceVariant,
                containerColor = c.surface
            ),
            border = BorderStroke(1.dp, c.outlineVariant)
        )
        AssistChip(
            onClick = {},
            label = { Text("Wake: ${userProfile.wakeUpTime}") },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = c.onSurfaceVariant,
                containerColor = c.surface
            ),
            border = BorderStroke(1.dp, c.outlineVariant)
        )
        AssistChip(
            onClick = {},
            label = { Text("Sleep: ${userProfile.sleepTime}") },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = c.onSurfaceVariant,
                containerColor = c.surface
            ),
            border = BorderStroke(1.dp, c.outlineVariant)
        )
    }
}

@Composable
private fun GoalInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    container: Color,
    iconContainer: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center
            ) {
                val tint =
                    when (iconContainer) {
                        MaterialTheme.colorScheme.primaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
                        MaterialTheme.colorScheme.secondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
                        MaterialTheme.colorScheme.tertiaryContainer -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompleteStep(
    userProfile: UserProfile,
    onComplete: () -> Unit
) {
    val angle by rememberInfiniteTransition(label = "rot")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )

    val angleReverse by rememberInfiniteTransition(label = "rot")
        .animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(132.dp)
                .rotate(angle),
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier
                        .size(54.dp)
                        .rotate(angleReverse),
                    imageVector = Icons.Rounded.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Your personalized hydration plan is ready.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Daily Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = WaterCalculator.formatWaterAmount(userProfile.dailyWaterGoal),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(10.dp))
                AnimatedVisibility(visible = true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallRecapChip(
                            label = "${userProfile.reminderInterval} min",
                            container = MaterialTheme.colorScheme.surface,
                            border = MaterialTheme.colorScheme.outlineVariant
                        )
                        SmallRecapChip(
                            label = userProfile.activityLevel.getDisplayName(),
                            container = MaterialTheme.colorScheme.surface,
                            border = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onComplete,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(imageVector = Icons.Rounded.WaterDrop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Start hydrating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SmallRecapChip(
    label: String,
    container: Color,
    border: Color
) {
    Surface(
        color = container,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/* ------------ Helpers ------------ */

private fun Gender.displayName(): String = when (this) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
    else -> name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun AgeGroup.displayName(): String = when (this) {
    AgeGroup.YOUNG_ADULT_18_30 -> "18–30"
    AgeGroup.ADULT_31_50 -> "31–50"
    else -> name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

/* ---------------------------------- Previews ---------------------------------- */

@Preview
@Composable
fun GoalStepPreview() {
    val userProfile = UserProfile(
        name = "Preview User",
        gender = Gender.FEMALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 60
    )
    GoalStep(
        userProfile = userProfile,
        title = "Your Personalized Goal",
        description = "Based on your info, here’s your recommended daily water intake."
    )
}

@Preview
@Composable
fun CompleteStepPreview() {
    val userProfile = UserProfile(
        name = "Preview User",
        gender = Gender.MALE,
        ageGroup = AgeGroup.YOUNG_ADULT_18_30,
        activityLevel = ActivityLevel.ACTIVE,
        wakeUpTime = "06:00",
        sleepTime = "22:00",
        dailyWaterGoal = 3000.0,
        reminderInterval = 45
    )
    CompleteStep(
        userProfile = userProfile,
        onComplete = {}
    )
}
