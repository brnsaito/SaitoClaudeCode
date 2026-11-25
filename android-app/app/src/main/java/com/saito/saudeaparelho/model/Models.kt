package com.saito.saudeaparelho.model

enum class Severity(val weight: Int, val label: String) {
    OK(0, "OK"),
    INFO(2, "Dica"),
    WARNING(8, "Atenção"),
    CRITICAL(20, "Crítico")
}

enum class Category(val label: String) {
    BATTERY("Bateria"),
    PERFORMANCE("Desempenho"),
    STORAGE("Armazenamento"),
    STABILITY("Travamentos"),
    SECURITY("Segurança"),
    APPS("Apps")
}

/** Ações de correção que o app sabe executar ou para as quais sabe abrir a tela certa. */
enum class FixAction {
    KILL_BACKGROUND,
    CLEAR_OWN_CACHE,
    CLEAR_ALL_CACHE,
    SET_SCREEN_TIMEOUT,
    SET_AUTO_BRIGHTNESS,
    REDUCE_ANIMATIONS,
    DISABLE_ALWAYS_FINISH,
    OPEN_BATTERY_SAVER,
    OPEN_BATTERY_OPTIMIZATION,
    OPEN_STORAGE,
    OPEN_DISPLAY,
    OPEN_DARK_THEME,
    OPEN_LOCATION,
    OPEN_SYSTEM_UPDATE,
    OPEN_DEVELOPER_OPTIONS,
    OPEN_APP_DETAILS,
    UNINSTALL_APP,
    GRANT_USAGE_ACCESS,
    GRANT_WRITE_SETTINGS,
    GRANT_ALL_FILES
}

data class Fix(val label: String, val action: FixAction, val packageName: String? = null)

data class Finding(
    val id: String,
    val category: Category,
    val severity: Severity,
    val title: String,
    val detail: String,
    val fix: Fix? = null
)

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val plugType: String,
    val status: String,
    val health: String,
    val healthCode: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    val technology: String,
    val cycleCount: Int?,
    val currentNowMa: Int?,
    val chargeCounterMah: Int?,
    val estimatedFullMah: Int?,
    val designCapacityMah: Int?,
    val healthPercentEstimate: Int?,
    val drainPerHour: Float?,
    val idleDrainPerHour: Float?,
    val historyHours: Float,
    val chargeTimeRemainingMin: Long?
)

data class MemoryStats(
    val totalBytes: Long,
    val availBytes: Long,
    val thresholdBytes: Long,
    val lowMemory: Boolean
) {
    val freeFraction: Float get() = if (totalBytes > 0) availBytes.toFloat() / totalBytes else 0f
}

data class StorageStats(val totalBytes: Long, val availBytes: Long) {
    val freeFraction: Float get() = if (totalBytes > 0) availBytes.toFloat() / totalBytes else 0f
}

data class SystemInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val securityPatch: String,
    val patchAgeMonths: Long?,
    val uptimeMs: Long,
    val cpuCores: Int,
    val cpuMaxFreqMhz: Int?,
    val thermalStatus: Int,
    val thermalLabel: String,
    val thermalHeadroom: Float?,
    val powerSaveMode: Boolean,
    val screenTimeoutMs: Int?,
    val brightness: Int?,
    val autoBrightness: Boolean?,
    val animatorScale: Float,
    val transitionScale: Float,
    val windowScale: Float,
    val alwaysFinishActivities: Boolean,
    val locationEnabled: Boolean,
    val darkMode: Boolean,
    val wifiScanAlways: Boolean?
)

data class AppRef(val packageName: String, val label: String)

data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMs: Long,
    val fgServiceMs: Long,
    val lastUsed: Long
)

data class AppStorage(
    val packageName: String,
    val label: String,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = appBytes + dataBytes
}

data class UnusedApp(
    val packageName: String,
    val label: String,
    val lastUsed: Long?,
    val installTime: Long,
    val sizeBytes: Long?
)

data class CrashRecord(
    val packageName: String,
    val label: String,
    val crashes: Int,
    val anrs: Int,
    val nativeCrashes: Int,
    val lastTime: Long
) {
    val total: Int get() = crashes + anrs + nativeCrashes
}

data class CrashReport(
    val apps: List<CrashRecord>,
    val systemEvents: Int,
    val days: Int
)

data class PermissionState(
    val usageAccess: Boolean,
    val writeSettings: Boolean,
    val writeSecureSettings: Boolean,
    val readLogs: Boolean,
    val allFilesAccess: Boolean
)

data class Report(
    val timestamp: Long,
    val battery: BatteryInfo,
    val memory: MemoryStats,
    val storage: StorageStats,
    val system: SystemInfo,
    val permissions: PermissionState,
    val topUsage: List<AppUsage>,
    val batteryExempt: List<AppRef>,
    val caches: List<AppStorage>,
    val totalCacheBytes: Long?,
    val unused: List<UnusedApp>,
    val crashes: CrashReport?,
    val userAppCount: Int,
    val findings: List<Finding>,
    val score: Int
)

/** Resultado de uma ação: mensagem para o usuário e/ou uma tela do sistema a abrir. */
data class ActionResult(
    val message: String? = null,
    val intent: android.content.Intent? = null,
    val forResult: Boolean = false
)
