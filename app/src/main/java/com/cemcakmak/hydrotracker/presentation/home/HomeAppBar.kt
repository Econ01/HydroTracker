package com.cemcakmak.hydrotracker.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    userProfile: UserProfile?,
    waterIntakeRepository: WaterIntakeRepository?
) {
    val elevated by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0f }
    }
    val animatedElevation by animateDpAsState(
        targetValue = if (elevated) 6.dp else 0.dp,
        label = "AppBarElevation"
    )

    Surface(
        tonalElevation = animatedElevation,
        shadowElevation = animatedElevation
    ) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (userProfile != null && waterIntakeRepository != null) {
                        HealthConnectSyncIcon(
                            userProfile = userProfile,
                            waterIntakeRepository = waterIntakeRepository,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
internal fun HealthConnectSyncIcon(
    userProfile: UserProfile,
    waterIntakeRepository: WaterIntakeRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var syncStatus by remember { mutableStateOf(HealthConnectSyncManager.SyncStatus.DISABLED) }

    val syncManager = remember { waterIntakeRepository.getSyncManager() }
    val isSyncing by syncManager.isSyncing.collectAsState()

    LaunchedEffect(userProfile.healthConnectSyncEnabled) {
        if (!userProfile.healthConnectSyncEnabled) {
            syncStatus = HealthConnectSyncManager.SyncStatus.DISABLED
            return@LaunchedEffect
        }

        syncStatus = try {
            when {
                !HealthConnectManager.isAvailable(context) -> HealthConnectSyncManager.SyncStatus.UNAVAILABLE
                !HealthConnectManager.hasPermissions(context) -> HealthConnectSyncManager.SyncStatus.NO_PERMISSIONS
                else -> HealthConnectSyncManager.SyncStatus.READY
            }
        } catch (_: Exception) {
            HealthConnectSyncManager.SyncStatus.ERROR
        }
    }

    AnimatedContent(
        targetState = Pair(syncStatus, isSyncing),
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
            fadeOut(animationSpec = tween(500))
        },
        modifier = modifier,
        label = "sync_icon_transition"
    ) { (status, syncing) ->
        when (status) {
            HealthConnectSyncManager.SyncStatus.READY -> {
                if (syncing) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = stringResource(R.string.cd_health_connect_syncing),
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = stringResource(R.string.cd_health_connect_synced),
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HealthConnectSyncManager.SyncStatus.DISABLED -> {
            }
            HealthConnectSyncManager.SyncStatus.UNAVAILABLE,
            HealthConnectSyncManager.SyncStatus.NO_PERMISSIONS -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = stringResource(R.string.cd_health_connect_unavailable),
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HealthConnectSyncManager.SyncStatus.ERROR -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = stringResource(R.string.cd_health_connect_error),
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
