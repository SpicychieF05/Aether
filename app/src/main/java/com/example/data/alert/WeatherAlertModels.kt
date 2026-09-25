package com.example.data.alert

import com.example.data.model.WeatherCondition

enum class AlertSeverity {
    NORMAL,
    ELEVATED,
    OFFICIAL_WARNING
}

data class WeatherAlertEvent(
    val eventId: String,
    val title: String,
    val body: String,
    val ctaText: String,
    val condition: WeatherCondition,
    val iconResId: Int,
    val focusedHourOfDay: Int?,
    val startTimeLabel: String,
    val durationHours: Int,
    val severity: AlertSeverity = AlertSeverity.NORMAL,
    val isOfficialWarning: Boolean = false,
    val materialHash: String,
    val timestamp: Long = System.currentTimeMillis()
)
