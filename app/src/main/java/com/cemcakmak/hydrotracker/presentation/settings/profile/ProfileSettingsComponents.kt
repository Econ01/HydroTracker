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

package com.cemcakmak.hydrotracker.presentation.settings.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.timeBasedGreeting
import com.cemcakmak.hydrotracker.presentation.common.LocalNavAnimatedVisibilityScope
import com.cemcakmak.hydrotracker.presentation.common.LocalSharedTransitionScope
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.presentation.settings.SelectableOptionCard
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.ImageUtils
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.io.File
import kotlin.math.round

/**
 * Profile settings UI components shared between the hub card and the detail screen.
 */

private val HERO_OUTER_RADIUS = 30.dp
private val HERO_AVATAR_SIZE = 72.dp

private const val GOAL_MIN_ML = 1500f
private const val GOAL_MAX_ML = 5000f
private const val GOAL_STEP_ML = 50f



@Composable
internal fun ProfileHeroPreview(
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    todayEntryCount: Int,
    daysTracked: Int,
    todayGoalProgress: Float,
    onEditProfilePicture: () -> Unit,
    onEditUsername: () -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    val nameModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "profile-name-${userProfile.name}"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    } else {
        Modifier
    }

    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val border = if (themePreferences.usePureBlack && isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HERO_OUTER_RADIUS),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile icon
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEditProfilePicture
                    )
                ) {
                    ProfileAvatar(
                        profileImagePath = userProfile.profileImagePath,
                        name = userProfile.name,
                        size = HERO_AVATAR_SIZE
                    )
                }

                // Personal message
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onEditUsername
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = timeBasedGreeting(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = userProfile.name,
                        modifier = nameModifier,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterHorizontally)
            ) {
                AnimatedStatItem(
                    value = todayEntryCount.toDouble(),
                    label = stringResource(R.string.profile_stat_today_entries)
                )

                VerticalDivider(modifier = Modifier.height(52.dp))

                AnimatedStatItem(
                    value = daysTracked.toDouble(),
                    label = stringResource(R.string.profile_stat_days_tracked)
                )

                VerticalDivider(modifier = Modifier.height(52.dp))

                AnimatedStatItem(
                    value = (todayGoalProgress * 100).toDouble(),
                    suffix = "%",
                    label = stringResource(R.string.profile_stat_today_goal)
                )
            }
        }
    }
}

/**
 * Profile Avatar Component with image support and fallback initials
 */
@Composable
fun ProfileAvatar(
    profileImagePath: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    var profileBitmap by remember(profileImagePath) {
        mutableStateOf(
            if (profileImagePath != null && File(profileImagePath).exists()) {
                ImageUtils.loadProfileImageBitmap(context, profileImagePath)?.asImageBitmap()
            } else {
                null
            }
        )
    }

    // Asynchronous fallback in case the synchronous initial load missed the cache.
    LaunchedEffect(profileImagePath) {
        if (profileBitmap == null && profileImagePath != null && File(profileImagePath).exists()) {
            profileBitmap = ImageUtils.loadProfileImageBitmap(context, profileImagePath)?.asImageBitmap()
        }
    }

    val sharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "profile-avatar-$name"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(sharedElementModifier)
            .size(size)
            .let { mod ->
                onClick?.let { mod.clickable { it() } } ?: mod
            },
        shape = CircleShape,
        color = if (profileBitmap != null) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        border = if (profileBitmap != null) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        } else null
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            profileBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.cd_profile_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                // Show initials as fallback
                Text(
                    text = getInitials(name),
                    style = MaterialTheme.typography.headlineMediumEmphasized
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

@Composable
private fun AnimatedStatItem(
    value: Double,
    label: String,
    suffix: String = ""
) {
    val animatedValue = rememberAnimatedDouble(
        targetValue = value,
        hapticsEnabled = true,
        animationSpec = tween(durationMillis = 800, easing = EaseInOut)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${animatedValue.toInt()}$suffix",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun GenderBottomSheetContent(
    currentGender: Gender,
    onGenderChange: (Gender) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_gender_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Gender.entries.forEachIndexed { index, gender ->
            SelectableOptionCard(
                index = index,
                size = Gender.entries.size,
                selected = gender == currentGender,
                onClick = {
                    onGenderChange(gender)
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            ) { contentColor ->
                Text(
                    text = stringResource(gender.labelResId),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GenderBottomSheet(
    currentGender: Gender,
    onGenderChange: (Gender) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        GenderBottomSheetContent(
            currentGender = currentGender,
            onGenderChange = onGenderChange
        )
    }
}

@Composable
internal fun ActivityLevelBottomSheetContent(
    currentLevel: ActivityLevel,
    onActivityLevelChange: (ActivityLevel) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_activity_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ActivityLevel.entries.forEachIndexed { index, level ->
            SelectableOptionCard(
                index = index,
                size = ActivityLevel.entries.size,
                selected = level == currentLevel,
                onClick = {
                    onActivityLevelChange(level)
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(level.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (level == currentLevel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        text = stringResource(level.descriptionResId),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityLevelBottomSheet(
    currentLevel: ActivityLevel,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ActivityLevelBottomSheetContent(
            currentLevel = currentLevel,
            onActivityLevelChange = onActivityLevelChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DailyGoalBottomSheetContent(
    currentGoalMl: Double,
    volumeUnit: VolumeUnit,
    onGoalChange: (Double) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var sliderPosition by remember { mutableFloatStateOf(currentGoalMl.toFloat()) }

    // Animate the displayed value whenever the slider snaps.
    val animatedGoalMl = rememberAnimatedDouble(
        targetValue = sliderPosition.toDouble(),
        hapticsEnabled = false,
        animationSpec = tween(durationMillis = 150, easing = EaseInOut)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_goal_edit_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = stringResource(R.string.profile_goal_edit_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Rolling number headline
        Text(
            text = stringResource(R.string.unit_milliliters_format, animatedGoalMl.toInt().toString()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Custom slider
        val startIcon = rememberVectorPainter(ImageVector.vectorResource(R.drawable.glass_cup_filled))
        val endIcon = rememberVectorPainter(ImageVector.vectorResource(R.drawable.water_bottle_large_filled))

        Slider(
            value = sliderPosition,
            onValueChange = { rawValue ->
                val snappedMl = round(rawValue / GOAL_STEP_ML) * GOAL_STEP_ML
                val newValue = snappedMl.coerceIn(GOAL_MIN_ML, GOAL_MAX_ML)
                if (newValue != sliderPosition) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    sliderPosition = newValue
                }
            },
            onValueChangeFinished = {
                onGoalChange(sliderPosition.toDouble())
            },
            valueRange = GOAL_MIN_ML..GOAL_MAX_ML,
            steps = 0,
            track = { sliderState ->
                val iconSize = DpSize(25.dp, 25.dp)
                val iconPadding = 10.dp
                val thumbTrackGapSize = 6.dp
                val activeIconColor = SliderDefaults.colors().activeTickColor
                val inactiveIconColor = SliderDefaults.colors().inactiveTickColor

                val trackIconStart: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x + iconPadding.toPx(), offset.y) {
                        with(startIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }
                val trackIconEnd: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x - iconPadding.toPx() - iconSize.toSize().width, offset.y) {
                        with(endIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }

                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier
                        .height(36.dp)
                        .drawWithContent {
                            drawContent()

                            val yOffset = size.height / 2 - iconSize.toSize().height / 2
                            val activeTrackEnd = size.width * sliderState.coercedValueAsFraction - thumbTrackGapSize.toPx()
                            val inactiveTrackStart = activeTrackEnd + thumbTrackGapSize.toPx() * 2
                            val inactiveTrackWidth = size.width - inactiveTrackStart

                            if (iconSize.toSize().width * 2 < activeTrackEnd - iconPadding.toPx() * 2) {
                                trackIconStart(Offset(0f, yOffset), activeIconColor)
                                trackIconEnd(Offset(activeTrackEnd, yOffset), activeIconColor)
                            }
                            if (iconSize.toSize().width * 2 < inactiveTrackWidth - iconPadding.toPx() * 2) {
                                trackIconStart(Offset(inactiveTrackStart, yOffset), inactiveIconColor)
                                trackIconEnd(Offset(size.width, yOffset), inactiveIconColor)
                            }
                        },
                    trackCornerSize = 12.dp,
                    drawStopIndicator = null,
                    thumbTrackGapSize = thumbTrackGapSize
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = VolumeUnitConverter.format(context, GOAL_MIN_ML.toDouble(), volumeUnit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = VolumeUnitConverter.format(context, GOAL_MAX_ML.toDouble(), volumeUnit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyGoalBottomSheet(
    currentGoalMl: Double,
    volumeUnit: VolumeUnit,
    onGoalChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        DailyGoalBottomSheetContent(
            currentGoalMl = currentGoalMl,
            volumeUnit = volumeUnit,
            onGoalChange = onGoalChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateProfilePictureBottomSheet(
    onImageSelected: (Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val context = LocalContext.current
        val haptics = LocalHapticFeedback.current

        // Camera launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                // Pass the temporary file URI to the cropper.
                val tempFile = File(context.cacheDir, "temp_profile_photo.jpg")
                if (tempFile.exists()) {
                    onImageSelected(Uri.fromFile(tempFile))
                }
            }
        }

        // Camera permission launcher
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, launch camera immediately
                val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraLauncher.launch(photoUri)
            }
        }

        // Image picker launcher
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { selectedUri ->
                // Pass the original URI to the cropper.
                onImageSelected(selectedUri)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_photo_sheet_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Select from gallery
            Column {
                com.cemcakmak.hydrotracker.presentation.settings.SettingsGroupCard(
                    index = 0,
                    size = 2,
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.photo_library_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_photo_sheet_gallery_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Take a photo
                com.cemcakmak.hydrotracker.presentation.settings.SettingsGroupCard(
                    index = 1,
                    size = 2,
                    onClick = {
                        // Check camera permission
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                // Create a temporary file for the camera image
                                val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
                                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(photoUri)
                            }

                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.photo_camera_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_photo_sheet_camera_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Delete profile picture button
            val deleteInteractionSource = remember { MutableInteractionSource() }

            LaunchedEffect(deleteInteractionSource) {
                deleteInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        else -> {  }
                    }
                }
            }

            Button(
                onClick = {
                    ImageUtils.deleteProfileImage(context)
                    onImageSelected(null)
                },
                interactionSource = deleteInteractionSource,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                    disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                ),
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.delete_fill),
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(R.string.profile_photo_sheet_delete),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UsernameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val isValidName = name.isNotBlank() && name.length <= 16

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.profile_name_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        if (newName.length <= 16) {
                            name = newName
                        }
                    },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    supportingText = {
                        Text(stringResource(R.string.character_count, name.length, 16))
                    },
                    isError = !isValidName,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    singleLine = true
                )

                if (!isValidName) {
                    Text(
                        text = if (name.isBlank()) {
                            stringResource(R.string.profile_name_error_empty)
                        } else {
                            stringResource(R.string.profile_name_error_too_long)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val haptics = LocalHapticFeedback.current
            val cancelInteractionSource = remember { MutableInteractionSource() }
            val confirmInteractionSource = remember { MutableInteractionSource() }

            LaunchedEffect(cancelInteractionSource) {
                cancelInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        else -> {  }
                    }
                }
            }

            LaunchedEffect(confirmInteractionSource) {
                confirmInteractionSource.interactions.collect { interaction ->
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
                                onConfirm(name)
                            },
                            enabled = isValidName,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shapes = ButtonDefaults.shapes(),
                            interactionSource = confirmInteractionSource,
                            modifier = with(scope) {
                                Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .animateWidth(interactionSource = confirmInteractionSource)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.action_save),
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

private val previewProfile = UserProfile(
    name = "Preview User",
    gender = Gender.MALE,
    ageGroup = AgeGroup.YOUNG_ADULT_18_30,
    activityLevel = ActivityLevel.MODERATE,
    wakeUpTime = "07:00",
    sleepTime = "23:00",
    dailyWaterGoal = 2500.0,
    reminderInterval = 120
)

@Preview(showBackground = true, name = "Profile Hero Preview - Light")
@Composable
fun ProfileHeroPreviewLightPreview() {
    HydroTrackerTheme {
        ProfileHeroPreview(
            userProfile = previewProfile,
            themePreferences = ThemePreferences(),
            todayEntryCount = 5,
            daysTracked = 12,
            todayGoalProgress = 0.65f,
            onEditProfilePicture = {},
            onEditUsername = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Hero Preview - Dark")
@Composable
fun ProfileHeroPreviewDarkPreview() {
    HydroTrackerTheme(
        themePreferences = ThemePreferences(darkMode = DarkModePreference.DARK)
    ) {
        ProfileHeroPreview(
            userProfile = previewProfile,
            themePreferences = ThemePreferences(darkMode = DarkModePreference.DARK),
            todayEntryCount = 5,
            daysTracked = 12,
            todayGoalProgress = 0.65f,
            onEditProfilePicture = {},
            onEditUsername = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Gender Bottom Sheet")
@Composable
fun GenderBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                var selectedGender by remember { mutableStateOf(Gender.MALE) }
                GenderBottomSheetContent(
                    currentGender = selectedGender,
                    onGenderChange = { selectedGender = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Activity Level Bottom Sheet")
@Composable
fun ActivityLevelBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                var selectedLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
                ActivityLevelBottomSheetContent(
                    currentLevel = selectedLevel,
                    onActivityLevelChange = { selectedLevel = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Daily Goal Bottom Sheet", heightDp = 700)
@Composable
fun DailyGoalBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                DailyGoalBottomSheetContent(
                    currentGoalMl = 2500.0,
                    volumeUnit = VolumeUnit.MILLILITRES,
                    onGoalChange = {}
                )
            }
        }
    }
}
