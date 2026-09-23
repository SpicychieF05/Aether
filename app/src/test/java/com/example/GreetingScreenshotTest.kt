package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.example.ui.components.AetherTopBar
import com.example.ui.diorama.DioramaCanvas
import com.example.ui.theme.AetherTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun diorama_topbar_screenshot() {
        composeTestRule.setContent {
            AetherTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    DioramaCanvas(
                        terrain = TerrainCategory.COASTAL,
                        dayFactor = 1.0f,
                        modifier = Modifier.fillMaxSize()
                    )
                    AetherTopBar(
                        location = LocationItem(
                            name = "Kolkata",
                            country = "India",
                            latitude = 22.57,
                            longitude = 88.36,
                            terrainCategory = TerrainCategory.COASTAL
                        ),
                        onLocationClick = {},
                        onSearchClick = {},
                        onSettingsClick = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/diorama_preview.png")
    }
}
