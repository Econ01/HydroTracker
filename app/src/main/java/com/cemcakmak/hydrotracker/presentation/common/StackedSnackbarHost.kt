// StackedSnackbarHost.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/common/StackedSnackbarHost.kt

package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Enhanced Snackbar Host that supports stacking multiple snackbars
 * Uses a global state to manage multiple simultaneous snackbars
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StackedSnackbarHost(
    modifier: Modifier = Modifier,
    maxVisibleSnackbars: Int = 3
) {
    val snackbarQueue by SnackbarQueue.snackbars.collectAsState()
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show only the most recent snackbars (up to maxVisible)
        val visibleSnackbars = snackbarQueue.takeLast(maxVisibleSnackbars)
        
        // Display snackbars from oldest to newest (bottom to top)
        visibleSnackbars.forEachIndexed { index, snackbar ->
            val isTopSnackbar = index == visibleSnackbars.size - 1
            
            key(snackbar.id) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // Slide in from bottom
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(400)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // Slide out downward
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    ) + fadeOut(animationSpec = tween(300)) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(300)
                    )
                ) {
                    StackedSnackbarItem(
                        snackbar = snackbar,
                        stackIndex = index,
                        isTopSnackbar = isTopSnackbar,
                        onDismiss = { SnackbarQueue.removeSnackbar(snackbar.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StackedSnackbarItem(
    snackbar: HydroSnackbar,
    stackIndex: Int,
    isTopSnackbar: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after duration
    LaunchedEffect(snackbar.id) {
        val delayMs = when (snackbar.duration) {
            SnackbarDuration.Short -> 4000L
            SnackbarDuration.Long -> 10000L
            SnackbarDuration.Indefinite -> return@LaunchedEffect
        }
        delay(delayMs)
        onDismiss()
    }
    
    // No visual scaling or transparency differences - all snackbars look the same
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(
                width = 1.dp,
                color = when (snackbar.type) {
                    HydroSnackbarType.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    HydroSnackbarType.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    HydroSnackbarType.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    HydroSnackbarType.INFO -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (snackbar.type) {
                HydroSnackbarType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                HydroSnackbarType.ERROR -> MaterialTheme.colorScheme.errorContainer
                HydroSnackbarType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                HydroSnackbarType.INFO -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Remove elevation to avoid rectangular shadows
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on snackbar type
            Icon(
                imageVector = when (snackbar.type) {
                    HydroSnackbarType.SUCCESS -> Icons.Default.CheckCircle
                    HydroSnackbarType.ERROR -> Icons.Default.Error
                    HydroSnackbarType.WARNING -> Icons.Default.Warning
                    HydroSnackbarType.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (snackbar.type) {
                    HydroSnackbarType.SUCCESS -> MaterialTheme.colorScheme.primary
                    HydroSnackbarType.ERROR -> MaterialTheme.colorScheme.error
                    HydroSnackbarType.WARNING -> MaterialTheme.colorScheme.tertiary
                    HydroSnackbarType.INFO -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(20.dp)
            )
            
            // Message text
            Text(
                text = snackbar.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when (snackbar.type) {
                    HydroSnackbarType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                    HydroSnackbarType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                    HydroSnackbarType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                    HydroSnackbarType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.weight(1f)
            )
            
            // Action button if present
            snackbar.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { 
                        snackbar.onAction?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = when (snackbar.type) {
                            HydroSnackbarType.SUCCESS -> MaterialTheme.colorScheme.primary
                            HydroSnackbarType.ERROR -> MaterialTheme.colorScheme.error
                            HydroSnackbarType.WARNING -> MaterialTheme.colorScheme.tertiary
                            HydroSnackbarType.INFO -> MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Data classes for stacked snackbars
 */
data class HydroSnackbar(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: HydroSnackbarType,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class HydroSnackbarType {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Global snackbar queue manager
 */
object SnackbarQueue {
    private val _snackbars = MutableStateFlow<List<HydroSnackbar>>(emptyList())
    val snackbars: StateFlow<List<HydroSnackbar>> = _snackbars.asStateFlow()
    
    fun addSnackbar(snackbar: HydroSnackbar) {
        _snackbars.value += snackbar
    }
    
    fun removeSnackbar(id: String) {
        _snackbars.value = _snackbars.value.filter { it.id != id }
    }
    
    fun clearAll() {
        _snackbars.value = emptyList()
    }
}

/**
 * Utility functions for easy snackbar creation
 */
fun showStackedSuccessSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    SnackbarQueue.addSnackbar(
        HydroSnackbar(
            message = message,
            type = HydroSnackbarType.SUCCESS,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    )
}

fun showStackedErrorSnackbar(
    message: String,
    actionLabel: String? = "Retry",
    onAction: (() -> Unit)? = null,
    duration: SnackbarDuration = SnackbarDuration.Long
) {
    SnackbarQueue.addSnackbar(
        HydroSnackbar(
            message = message,
            type = HydroSnackbarType.ERROR,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    )
}

fun showStackedWarningSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    duration: SnackbarDuration = SnackbarDuration.Long
) {
    SnackbarQueue.addSnackbar(
        HydroSnackbar(
            message = message,
            type = HydroSnackbarType.WARNING,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    )
}

fun showStackedInfoSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    SnackbarQueue.addSnackbar(
        HydroSnackbar(
            message = message,
            type = HydroSnackbarType.INFO,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    )
}