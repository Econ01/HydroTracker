/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.statistics.ContainerUsageItem
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.NumberFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape

/**
 * A grouped card that lists container usage, ordered by total volume.
 *
 * Each row shows the container name, how many times it was used, the total volume, and a small
 * progress bar relative to the most-used container.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainerUsageCard(
    items: List<ContainerUsageItem>,
    volumeUnit: VolumeUnit
) {
    val context = LocalContext.current

    Column {
        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    shape = SquircleShape(
                        topStart = CornerSize(24.dp),
                        topEnd = CornerSize(10.dp),
                        bottomEnd = CornerSize(10.dp),
                        bottomStart = CornerSize(10.dp)
                    ),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(2f)
                        .padding(bottom = 3.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .weight(2f)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        text = stringResource(R.string.statistics_container_header_container),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }

                Surface(
                    shape = SquircleShape(
                        topStart = CornerSize(10.dp),
                        topEnd = CornerSize(10.dp),
                        bottomEnd = CornerSize(10.dp),
                        bottomStart = CornerSize(10.dp)
                    ),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 3.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        text = stringResource(R.string.statistics_container_header_volume),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }

                Surface(
                    shape = SquircleShape(
                        topStart = CornerSize(10.dp),
                        topEnd = CornerSize(24.dp),
                        bottomEnd = CornerSize(10.dp),
                        bottomStart = CornerSize(10.dp)
                    ),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 3.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        text = stringResource(R.string.statistics_container_header_uses),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            }
        }

        items.forEachIndexed { index, item ->
            val shapeLeft = containerRowShape(index, items.size, ContainerColumn.START)
            val shapeCenter = containerRowShape(index, items.size, ContainerColumn.CENTER)
            val shapeRight = containerRowShape(index, items.size, ContainerColumn.END)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    shape = shapeLeft,
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .weight(2f)
                        .padding(bottom = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(item.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = shapeCenter,
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 3.dp)
                ) {
                    val volumeText = VolumeUnitConverter.format(context, item.volume, volumeUnit)

                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                        text = volumeText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = shapeRight,
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 3.dp)
                ) {
                    val compactCount = NumberFormatters.formatCompactCount(item.count.toDouble())

                    Text(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        text = compactCount,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private enum class ContainerColumn {
    START,
    CENTER,
    END
}

private fun containerRowShape(
    index: Int,
    size: Int,
    column: ContainerColumn,
    outerRadius: Dp = 24.dp,
    innerRadius: Dp = 10.dp
): Shape {
    if (size == 1) return PillShape

    val isBottomRow = index == size - 1
    val isOuterStart = column == ContainerColumn.START
    val isOuterEnd = column == ContainerColumn.END

    return SquircleShape(
        topStart = CornerSize(innerRadius),
        topEnd = CornerSize(innerRadius),
        bottomStart = CornerSize(if (isBottomRow && isOuterStart) outerRadius else innerRadius),
        bottomEnd = CornerSize(if (isBottomRow && isOuterEnd) outerRadius else innerRadius)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Container Usage Card")
@Composable
private fun ContainerUsageCardPreview() {
    HydroTrackerTheme {
        ContainerUsageCard(
            items = listOf(
                ContainerUsageItem("Water Bottle", 8000, 40000.0, R.drawable.water_bottle_filled),
                ContainerUsageItem("Coffee Mug", 45000000, 9000.0, R.drawable.coffee_filled),
                ContainerUsageItem("Tea Cup", 30, 7200.0, R.drawable.tea_filled)
            ),
            volumeUnit = VolumeUnit.MILLILITRES
        )
    }
}
