package com.cemcakmak.hydrotracker.presentation.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File
import java.time.LocalTime
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.utils.WaterCalculator

/**
 * Profile Header Card with user avatar and quick stats
 */
@Composable
fun ProfileHeaderCard(
    userProfile: UserProfile,
    todayStatistics: TodayStatistics,
    totalDaysTracked: Int,
    onEditProfilePicture: () -> Unit = {},
    onEditUsername: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(
                        profileImagePath = userProfile.profileImagePath,
                        name = userProfile.name,
                        size = 56.dp,
                        onClick = onEditProfilePicture
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = getTimeBasedGreeting(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onEditUsername()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }


                ProfileStatCard(
                    value = "${(todayStatistics.goalProgress * 100).toInt()}%",
                    label = "Today's Goal",
                    isHighlight = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ProfileStatSegment(
                        value = "${todayStatistics.entryCount}",
                        label = "Entries",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider()

                    ProfileStatSegment(
                        value = "$totalDaysTracked",
                        label = "Days",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider()

                    ProfileStatSegment(
                        value = "${(todayStatistics.goalProgress * 100).toInt()}%",
                        label = "Goal",
                        modifier = Modifier.weight(1f),
                        isHighlight = true
                    )
                }
            }
            }



        }
    }
}
@Composable
fun ProfileStatSegment(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = if (isHighlight)
                MaterialTheme.typography.titleLarge
            else
                MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {


            withStyle(
                SpanStyle(
                    fontSize = if (isHighlight)
                        MaterialTheme.typography.headlineSmall.fontSize
                    else
                        MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighlight)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                append(value)
            }

            append(" ")


            withStyle(
                SpanStyle(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                append(label)
            }
        }
    )
}
@Composable
fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun QuickStatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Profile Avatar Component with image support and fallback initials
 */
@Composable
fun ProfileAvatar(
    profileImagePath: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var profileBitmap by remember(profileImagePath) { mutableStateOf<ImageBitmap?>(null) }
    
    // Load the image when profileImagePath changes
    LaunchedEffect(profileImagePath) {
        profileBitmap = if (profileImagePath != null && File(profileImagePath).exists()) {
            ImageUtils.loadProfileImageBitmap(context)?.asImageBitmap()
        } else {
            null
        }
    }
    
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .size(size)
            .let { mod -> 
                onClick?.let { mod.clickable { it() } } ?: mod 
            },
        shape = CircleShape,
        color = if (profileBitmap != null) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        },
        border = if (profileBitmap != null) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        } else null
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            profileBitmap?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                // Show initials as fallback
                Text(
                    text = getInitials(name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Get user's initials from their name
 */
private fun getInitials(name: String): String {
    return name.trim()
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "U" } // Fallback to "U" for User
}

/**
 * Get time-based greeting message
 */
private fun getTimeBasedGreeting(): String {
    val currentHour = LocalTime.now().hour
    return when (currentHour) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        in 17..21 -> "Good evening,"
        else -> "Hello,"
    }
}


/**
 * Profile Details Card - Personal information
 */
@Composable
fun ProfileDetailsCard(
    userProfile: UserProfile,
    onEditGender: () -> Unit,
    onEditAgeGroup: () -> Unit,
    onEditWeight: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val haptics = LocalHapticFeedback.current
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Gender
                EditableInfoRow(
                    icon = Icons.Default.Person,
                    label = "Gender",
                    value = userProfile.gender.getDisplayName(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditGender() }
                )

                // Age Group
                EditableInfoRow(
                    icon = Icons.Default.Cake,
                    label = "Age Group",
                    shape = RoundedCornerShape(4.dp),
                    value = userProfile.ageGroup.getDisplayName(),
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditAgeGroup() }
                )

                // Weight
                EditableInfoRow(
                    icon = Icons.Default.MonitorWeight,
                    label = "Weight",
                    shape =  RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    value = if (userProfile.weight != null) "${userProfile.weight.toInt()} kg" else "Not set",
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditWeight() }
                )



            }
        }
    }
}

/**
 * Daily Goals Card - Water goals and activity level
 */
@Composable
fun DailyGoalsCard(
    userProfile: UserProfile,
    onEditGoal: () -> Unit,
    onEditActivity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val haptics = LocalHapticFeedback.current
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Daily Goal
                EditableInfoRow(
                    icon = Icons.Default.WaterDrop,
                    label = "Daily Water Goal",
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    value = WaterCalculator.formatWaterAmount(userProfile.dailyWaterGoal),
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditGoal() }
                )

                // Activity Level
                EditableInfoRow(
                    icon = Icons.Default.FitnessCenter,
                    label = "Activity Level",
                    value = userProfile.activityLevel.getDisplayName(),
                    shape =  RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditActivity() }
                )


            }
        }
    }
}

/**
 * Active Schedule Card - Wake/sleep times and reminders
 */
@Composable
fun ActiveScheduleCard(
    userProfile: UserProfile,
    onEditSchedule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Schedule",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val haptics = LocalHapticFeedback.current
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Schedule
                EditableInfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "Active Hours",
                    shape =  RoundedCornerShape(
                        16.dp
                    ),
                    value = "${userProfile.wakeUpTime} - ${userProfile.sleepTime}",
                    onClick = {haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditSchedule() }
                )

                // Reminder Frequency (Read-only)
                InfoRow(
                    icon = Icons.Default.Notifications,
                    label = "Reminder Interval",
                    value = "Every ${userProfile.reminderInterval} minutes"
                )
            }
        }
    }
}

