package com.saito.saudeaparelho.data

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.storage.StorageManager
import com.saito.saudeaparelho.model.AppRef
import com.saito.saudeaparelho.model.AppStorage
import com.saito.saudeaparelho.model.AppUsage
import com.saito.saudeaparelho.model.UnusedApp

class AppAnalyzer(private val ctx: Context) {

    private val pm: PackageManager = ctx.packageManager
    private val labelCache = HashMap<String, String>()

    /** Apps instalados pelo usuário (inclui atualizações de apps de sistema), exceto este. */
    val userApps: List<ApplicationInfo> by lazy {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0).filter {
            it.packageName != ctx.packageName &&
                ((it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
        }
    }

    val allPackages: List<String> by lazy {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0).map { it.packageName }
    }

    fun label(pkg: String): String = labelCache.getOrPut(pkg) {
        runCatching {
            @Suppress("DEPRECATION")
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }

    /** Apps mais usados no último dia: tempo em primeiro plano e com serviço em primeiro plano. */
    fun usageLastDay(limit: Int = 15): List<AppUsage> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val now = System.currentTimeMillis()
        val stats = runCatching { usm.queryAndAggregateUsageStats(now - DAY, now) }.getOrNull() ?: return emptyList()
        return stats.values
            .filter { it.packageName != ctx.packageName }
            .map { s ->
                val fgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) s.totalTimeForegroundServiceUsed else 0L
                AppUsage(s.packageName, label(s.packageName), s.totalTimeInForeground, fgs, s.lastTimeUsed)
            }
            .filter { it.foregroundMs + it.fgServiceMs > 60_000 }
            .sortedByDescending { it.foregroundMs + it.fgServiceMs }
            .take(limit)
    }

    /** Apps que não foram abertos nos últimos [days] dias (instalados há mais tempo que isso). */
    fun unused(days: Int = 30, sizes: Map<String, Long> = emptyMap()): List<UnusedApp> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val now = System.currentTimeMillis()
        val since = now - days * DAY
        val stats = runCatching { usm.queryAndAggregateUsageStats(since, now) }.getOrNull() ?: return emptyList()
        if (stats.isEmpty()) return emptyList()
        return userApps.mapNotNull { ai ->
            val install = runCatching {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ai.packageName, 0).firstInstallTime
            }.getOrDefault(now)
            if (install > since) return@mapNotNull null
            val s = stats[ai.packageName]
            val last = s?.lastTimeUsed?.takeIf { it > 0 }
            val usedRecently = s != null && (s.totalTimeInForeground > 0 || (last != null && last >= since))
            if (usedRecently) null
            else UnusedApp(ai.packageName, label(ai.packageName), last, install, sizes[ai.packageName])
        }.sortedByDescending { it.sizeBytes ?: 0L }
    }

    /** Apps liberados da otimização de bateria (podem rodar livremente em segundo plano). */
    fun batteryExempt(): List<AppRef> {
        val power = ctx.getSystemService(PowerManager::class.java) ?: return emptyList()
        return userApps
            .filter { runCatching { power.isIgnoringBatteryOptimizations(it.packageName) }.getOrDefault(false) }
            .map { AppRef(it.packageName, label(it.packageName)) }
            .sortedBy { it.label.lowercase() }
    }

    /** Tamanho de app, dados e cache por app. Requer acesso de uso; retorna null sem permissão. */
    fun storage(): List<AppStorage>? {
        val ssm = ctx.getSystemService(StorageStatsManager::class.java) ?: return null
        val user = Process.myUserHandle()
        val result = ArrayList<AppStorage>()
        for (ai in userApps) {
            try {
                val s = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, user)
                result += AppStorage(ai.packageName, label(ai.packageName), s.appBytes, s.dataBytes, s.cacheBytes)
            } catch (_: SecurityException) {
                return null
            } catch (_: Exception) {
                // pacote removido durante a análise, etc.
            }
        }
        return result
    }

    companion object {
        const val DAY = 24L * 3600 * 1000
    }
}
