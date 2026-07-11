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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

/**
 * Large hero statistic used in the top row of the Statistics screen.
 *
 * Displays a bold animated value above a short label, styled to match the app's chart stat items.
 * When [tooltipText] is provided, the exact value is shown in a tooltip on long-press.
 * When [suffix] is provided, it is rendered using [suffixStyle] so the number and suffix can
 * have different font sizes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroStatItem(
    label: String,
    value: Double,
    shape: SquircleShape,
    hapticsEnabled: Boolean,
    formatValue: @Composable (Float) -> String,
    tooltipText: String? = null,
    suffix: String? = null,
    suffixStyle: TextStyle = MaterialTheme.typography.headlineSmallEmphasized
) {
    val animatedValue = rememberAnimatedDouble(
        targetValue = value,
        hapticsEnabled = hapticsEnabled
    )

    val annotatedValue = buildAnnotatedString {
        append(formatValue(animatedValue))
        if (!suffix.isNullOrBlank()) {
            withStyle(style = suffixStyle.toSpanStyle()) {
                append(suffix)
            }
        }
    }

    val content = @Composable {
        Surface(
            modifier = Modifier
                .height(120.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = annotatedValue,
                    style = MaterialTheme.typography.displaySmallEmphasized
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                )
            }
        }
    }

    if (tooltipText.isNullOrBlank()) {
        content()
    } else {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
            tooltip = {
                PlainTooltip {
                    Text(tooltipText)
                }
            },
            state = rememberTooltipState()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Hero Stat Item")
@Composable
private fun HeroStatItemPreview() {
    HydroTrackerTheme {
        HeroStatItem(
            label = "Current Streak",
            value = 12.0,
            shape = SquircleShape(),
            hapticsEnabled = false,
            formatValue = { it.toInt().toString() }
        )
    }
}
