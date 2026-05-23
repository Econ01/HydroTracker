package com.dev.hydrotracker.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.dev.hydrotracker.data.models.*

@Composable
fun GenderStep(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    title: String,
    description: String
) {
    OnboardingStepLayout(
        title = title,
        description = description
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Gender.entries.forEach { gender ->
                SelectionCard(
                    title = gender.getDisplayName(),
                    description = gender.getPersonalizedGreeting(),
                    isSelected = selectedGender == gender,
                    onClick = { onGenderSelected(gender) },
                    icon = when (gender) {
                        Gender.MALE -> "👨"
                        Gender.FEMALE -> "👩"
                        Gender.OTHER -> "👤"
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun GenderStepPreview() {
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    GenderStep(
        selectedGender = selectedGender,
        onGenderSelected = { gender ->
            selectedGender = gender
        },
        title = "Select Your Gender",
        description = "This helps us tailor your hydration recommendations."
    )
}


@Composable
fun AgeStep(
    selectedAgeGroup: AgeGroup,
    onAgeGroupSelected: (AgeGroup) -> Unit,
    title: String,
    description: String
) {
    OnboardingStepLayout(
        title = title,
        description = description
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AgeGroup.entries.forEach { ageGroup ->
                SelectionCard(
                    title = ageGroup.getDisplayName(),
                    description = ageGroup.getMotivationalMessage(),
                    isSelected = selectedAgeGroup == ageGroup,
                    onClick = { onAgeGroupSelected(ageGroup) },
                    icon = when (ageGroup) {
                        AgeGroup.YOUNG_ADULT_18_30 -> "🌟"
                        AgeGroup.ADULT_31_50 -> "💪"
                        AgeGroup.MIDDLE_AGED_51_60 -> "🌿"
                        AgeGroup.SENIOR_60_PLUS -> "🌸"
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun AgeStepPreview() {
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.YOUNG_ADULT_18_30) }
    AgeStep(
        selectedAgeGroup = selectedAgeGroup,
        onAgeGroupSelected = { ageGroup ->
            selectedAgeGroup = ageGroup
        },
        title = "Select Your Age Group",
        description = "Your age influences your hydration needs."
    )
}

@Composable
fun ActivityStep(
    selectedActivityLevel: ActivityLevel,
    onActivityLevelSelected: (ActivityLevel) -> Unit,
    title: String,
    description: String
) {
    OnboardingStepLayout(
        title = title,
        description = description
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActivityLevel.entries.forEach { activityLevel ->
                SelectionCard(
                    title = activityLevel.getDisplayName(),
                    description = activityLevel.getHydrationTip(),
                    isSelected = selectedActivityLevel == activityLevel,
                    onClick = { onActivityLevelSelected(activityLevel) },
                    icon = when (activityLevel) {
                        ActivityLevel.SEDENTARY -> "🪑"
                        ActivityLevel.LIGHT -> "🚶"
                        ActivityLevel.MODERATE -> "🏃"
                        ActivityLevel.ACTIVE -> "🏋️"
                        ActivityLevel.VERY_ACTIVE -> "🏃‍♂️"
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun ActivityStepPreview() {
    var selectedActivityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    ActivityStep(
        selectedActivityLevel = selectedActivityLevel,
        onActivityLevelSelected = { activityLevel ->
            selectedActivityLevel = activityLevel
        },
        title = "Select Your Activity Level",
        description = "Your activity level affects how much water you need."
    )
}
