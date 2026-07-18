package com.cemcakmak.hydrotracker.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cemcakmak.hydrotracker.R

@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    isVisible: Boolean
) {
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon with spring animation
        Card(
            shape = MaterialShapes.Cookie12Sided.toShape(),
            modifier = Modifier
                .size(120.dp)
                .scale(logoScale),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = stringResource(R.string.cd_water_drop),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Welcome content
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { it / 2 }
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.displayLargeEmphasized,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.onboarding_welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Features
        Column(modifier = Modifier.fillMaxWidth()) {
            FeatureItem(
                emoji = "🎯",
                title = stringResource(R.string.onboarding_feature_goals_title),
                description = stringResource(R.string.onboarding_feature_goals_desc),
                isVisible = isVisible,
                delay = 600
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureItem(
                emoji = "⏰",
                title = stringResource(R.string.onboarding_feature_reminders_title),
                description = stringResource(R.string.onboarding_feature_reminders_desc),
                isVisible = isVisible,
                delay = 800
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureItem(
                emoji = "📊",
                title = stringResource(R.string.onboarding_feature_progress_title),
                description = stringResource(R.string.onboarding_feature_progress_desc),
                isVisible = isVisible,
                delay = 1000
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Action buttons
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(600, delayMillis = 1200))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_get_started),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun WelcomeStepPreview() {
    WelcomeStep(
        onNext = {},
        isVisible = true
    )
}

@Preview
@Composable
fun FeatureItemPreview() {
    FeatureItem(
        emoji = "🎯",
        title = "Personalized Goals",
        description = "Based on scientific research and your profile",
        isVisible = true,
        delay = 600
    )
}


@Composable
private fun FeatureItem(
    emoji: String,
    title: String,
    description: String,
    isVisible: Boolean = true,
    delay: Int = 0
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetX = { -it }
        ) + fadeIn(animationSpec = tween(600, delayMillis = delay))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}