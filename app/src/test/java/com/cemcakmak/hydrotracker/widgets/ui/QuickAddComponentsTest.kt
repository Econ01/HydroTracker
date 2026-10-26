package com.cemcakmak.hydrotracker.widgets.ui

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.widgets.HydroWidgetState
import com.cemcakmak.hydrotracker.widgets.WidgetQuickAddSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class QuickAddComponentsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `custom beverage keeps its storage key label and accessible identity`() {
        val customName = "Berry electrolyte mix"
        val model = slotCardModel(
            context = context,
            slot = WidgetQuickAddSlot(
                volume = 250.0,
                beverageName = customName,
                containerName = "Glass",
            ),
            useDynamicColors = false,
        )

        assertEquals(customName, model.beverageName)
        assertEquals(customName, model.label)
        val description = quickAddContentDescription(context, testState, model)
        assertTrue(description.contains(customName))
        assertTrue(description.contains("250"))
    }

    @Test
    fun `standard and legacy beverages retain their canonical action keys`() {
        val coffee = slotCardModel(
            context,
            WidgetQuickAddSlot(200.0, BeverageType.COFFEE.name, "Cup"),
            useDynamicColors = false,
        )
        val legacyWater = slotCardModel(
            context,
            WidgetQuickAddSlot(200.0, "COCONUT_WATER", "Glass"),
            useDynamicColors = false,
        )

        assertEquals(BeverageType.COFFEE.name, coffee.beverageName)
        assertEquals(context.getString(BeverageType.COFFEE.labelResId), coffee.label)
        assertEquals(BeverageType.WATER.name, legacyWater.beverageName)
    }

    private val testState = HydroWidgetState(
        currentIntake = 0.0,
        dailyGoal = 2700.0,
        progress = 0f,
        isGoalAchieved = false,
        remainingAmount = 2700.0,
        volumeUnit = VolumeUnit.MILLILITRES,
    )
}
