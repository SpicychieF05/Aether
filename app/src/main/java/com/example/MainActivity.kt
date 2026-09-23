package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AetherMainScreen
import com.example.ui.AetherViewModel
import com.example.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AetherViewModel = viewModel()
            AetherTheme {
                AetherMainScreen(viewModel = viewModel)
            }
        }
    }
}
