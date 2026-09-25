package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.ui.AetherMainScreen
import com.example.ui.AetherViewModel
import com.example.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AetherViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldOpenSettings = intent?.getBooleanExtra("OPEN_SETTINGS_UPDATE", false) ?: false
        handleNotificationDeepLink(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AetherTheme {
                AetherMainScreen(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    initialOpenSettings = shouldOpenSettings
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeepLink(intent)
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra("EXTRA_FOCUS_HOUR")) {
            val focusHour = intent.getIntExtra("EXTRA_FOCUS_HOUR", -1)
            if (focusHour != -1) {
                viewModel.setScrubbedHourByHourOfDay(focusHour)
            }
        }
    }
}

