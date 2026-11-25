package com.saito.saudeaparelho.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.saito.saudeaparelho.model.Severity

val Good = Color(0xFF2ECC87)
val Warn = Color(0xFFFFB74D)
val Bad = Color(0xFFFF5252)
val InfoBlue = Color(0xFF3B9EFF)

fun severityColor(s: Severity) = when (s) {
    Severity.OK -> Good
    Severity.INFO -> InfoBlue
    Severity.WARNING -> Warn
    Severity.CRITICAL -> Bad
}

fun scoreColor(score: Int) = when {
    score >= 80 -> Good
    score >= 55 -> Warn
    else -> Bad
}

private val DarkScheme = darkColorScheme(
    primary = InfoBlue,
    secondary = Good,
    background = Color(0xFF0F1923),
    surface = Color(0xFF0F1923),
    surfaceContainer = Color(0xFF162130),
    surfaceContainerHigh = Color(0xFF1D2D3E),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1A6FC4),
    secondary = Color(0xFF1A8F5A),
)

@Composable
fun SaudeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
