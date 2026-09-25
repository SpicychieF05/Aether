package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherEffectType

object WeatherIconMapper {

    /**
     * Resolves the static vector drawable resource matching the provided weather condition
     * based on the official Aether icon set.
     */
    fun getIconDrawableRes(
        condition: WeatherCondition,
        isDay: Boolean = true,
        isOfficialWarning: Boolean = false
    ): Int {
        if (isOfficialWarning) {
            return R.drawable.ic_weather_warning
        }

        return when (condition.effectType) {
            WeatherEffectType.THUNDERSTORM -> {
                if (condition.rainIntensity >= 0.7f || condition.displayName.contains("Hail", ignoreCase = true)) {
                    R.drawable.ic_weather_severe_thunderstorm
                } else {
                    R.drawable.ic_weather_thunderstorms
                }
            }
            WeatherEffectType.RAIN -> {
                when {
                    condition.rainIntensity <= 0.35f -> R.drawable.ic_weather_rainy_1
                    condition.rainIntensity <= 0.7f -> R.drawable.ic_weather_rainy_2
                    else -> R.drawable.ic_weather_rainy_3
                }
            }
            WeatherEffectType.CLEAR -> {
                if (isDay) R.drawable.ic_weather_clear_day else R.drawable.ic_weather_clear_night
            }
            WeatherEffectType.CLOUDY -> {
                if (isDay && (condition.code in listOf(1, 2, 1100, 1101))) {
                    R.drawable.ic_weather_cloudy_day
                } else {
                    R.drawable.ic_weather_cloudy
                }
            }
            WeatherEffectType.FOG -> {
                if (condition.displayName.contains("Haze", ignoreCase = true)) {
                    R.drawable.ic_weather_haze
                } else {
                    R.drawable.ic_weather_fog
                }
            }
            WeatherEffectType.SNOW -> R.drawable.ic_weather_snowy
            WeatherEffectType.WINDY -> R.drawable.ic_weather_wind
        }
    }

    /**
     * Generates a high-resolution Bitmap from vector drawable suitable for
     * NotificationCompat.Builder.setLargeIcon(bitmap).
     */
    fun getLargeIconBitmap(
        context: Context,
        condition: WeatherCondition,
        isDay: Boolean = true,
        isOfficialWarning: Boolean = false,
        sizePx: Int = 128
    ): Bitmap? {
        val resId = getIconDrawableRes(condition, isDay, isOfficialWarning)
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
