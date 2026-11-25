package com.saito.saudeaparelho.data

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import com.saito.saudeaparelho.model.MemoryStats
import com.saito.saudeaparelho.model.StorageStats
import com.saito.saudeaparelho.model.SystemInfo
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SystemAnalyzer {

    fun memory(ctx: Context): MemoryStats {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        return MemoryStats(mi.totalMem, mi.availMem, mi.threshold, mi.lowMemory)
    }

    fun storage(): StorageStats {
        val st = StatFs(Environment.getDataDirectory().path)
        return StorageStats(st.totalBytes, st.availableBytes)
    }

    fun system(ctx: Context): SystemInfo {
        val cr = ctx.contentResolver
        val pm = ctx.getSystemService(PowerManager::class.java)

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pm?.currentThermalStatus ?: 0 else 0
        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { pm?.getThermalHeadroom(10) }.getOrNull()?.takeIf { !it.isNaN() }
        } else null

        val patchAge = runCatching {
            ChronoUnit.MONTHS.between(LocalDate.parse(Build.VERSION.SECURITY_PATCH), LocalDate.now())
        }.getOrNull()

        val cores = Runtime.getRuntime().availableProcessors()
        val maxFreq = (0 until cores).mapNotNull { i ->
            runCatching {
                File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toInt() / 1000
            }.getOrNull()
        }.maxOrNull()

        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ctx.getSystemService(LocationManager::class.java)?.isLocationEnabled ?: false
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE, 0) != 0
        }

        val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        return SystemInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            patchAgeMonths = patchAge,
            uptimeMs = SystemClock.elapsedRealtime(),
            cpuCores = cores,
            cpuMaxFreqMhz = maxFreq,
            thermalStatus = thermal,
            thermalLabel = thermalLabel(thermal),
            thermalHeadroom = headroom,
            powerSaveMode = pm?.isPowerSaveMode ?: false,
            screenTimeoutMs = runCatching { Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT) }.getOrNull(),
            brightness = runCatching { Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS) }.getOrNull(),
            autoBrightness = runCatching {
                Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }.getOrNull(),
            animatorScale = Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
            transitionScale = Settings.Global.getFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f),
            windowScale = Settings.Global.getFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE, 1f),
            alwaysFinishActivities = Settings.Global.getInt(cr, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1,
            locationEnabled = location,
            darkMode = night,
            wifiScanAlways = Settings.Global.getInt(cr, "wifi_scan_always_enabled", -1)
                .takeIf { it >= 0 }?.let { it == 1 }
        )
    }

    fun thermalLabel(status: Int) = when (status) {
        0 -> "Normal"
        1 -> "Leve"
        2 -> "Moderado"
        3 -> "Severo"
        4 -> "Crítico"
        5 -> "Emergência"
        6 -> "Desligamento"
        else -> "Desconhecido"
    }
}
