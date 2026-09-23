package com.example.ui.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * AdaptiveLayoutWrapper:
 * Canonical responsive Composable layout wrapper that evaluates the WindowWidthSizeClass
 * and smoothly switches between a vertical single-column layout for 'Compact' widths
 * and a side-by-side multi-pane dashboard layout for 'Expanded' and 'Medium' widths (tablets & foldables).
 */
@Composable
fun AdaptiveLayoutWrapper(
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
        expandedContent()
    } else {
        compactContent()
    }
}
