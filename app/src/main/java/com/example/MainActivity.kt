package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AetherMainScreen
import com.example.ui.AetherViewModel
import com.example.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldOpenSettings = intent?.getBooleanExtra("OPEN_SETTINGS_UPDATE", false) ?: false

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val viewModel: AetherViewModel = viewModel()
            AetherTheme {
                AetherMainScreen(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    initialOpenSettings = shouldOpenSettings
                )
            }
        }
    }
}
