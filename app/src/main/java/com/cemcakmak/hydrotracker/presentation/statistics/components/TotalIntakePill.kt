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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter.toDisplayUnit

/**
 * A large pill-shaped hero for the total all-time intake.
 *
 * The pill is filled with a static, water-themed mesh gradient that flows from light blues at the
 * top through teals and blues to deeper purple-blues at the bottom. The total intake value and
 * unit are centred on top.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TotalIntakePill(
    label: String,
    totalIntake: Double,
    volumeUnit: VolumeUnit,
    modifier: Modifier = Modifier
) {
    val displayUnit = VolumeUnitConverter.MetricEquivalent.LITRE.toDisplayUnit(volumeUnit)

    val animatedValue = rememberAnimatedDouble(
        targetValue = totalIntake,
        hapticsEnabled = true,
        step = displayUnit.toMillilitresFactor
    )

    val annotatedValue = buildAnnotatedString {
        append(VolumeUnitConverter.formatValue(animatedValue.toDouble(), displayUnit))
        withStyle(style = MaterialTheme.typography.headlineSmallEmphasized.toSpanStyle()) {
            append(stringResource(displayUnit.shortLabelResId))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = SquircleShape(
            topStart = CornerSize(40.dp),
            topEnd = CornerSize(40.dp),
            bottomStart = CornerSize(20.dp),
            bottomEnd = CornerSize(20.dp)
        ),
        color = MaterialTheme.extendedColorScheme.successContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = annotatedValue,
                style = MaterialTheme.typography.displayLargeEmphasized,
                color = MaterialTheme.extendedColorScheme.onSuccessContainer
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extendedColorScheme.onSuccessContainer.copy(0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Total Intake Pill")
@Composable
private fun TotalIntakePillPreview() {
    HydroTrackerTheme {
        TotalIntakePill(
            label = "Total Intake",
            totalIntake = 71_400_000.0,
            volumeUnit = VolumeUnit.MILLILITRES,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
