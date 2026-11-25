package com.saito.saudeaparelho.data

import android.content.Context
import com.saito.saudeaparelho.model.Report

object ReportBuilder {

    fun build(ctx: Context): Report {
        runCatching { BatteryHistory.record(ctx) }

        val perms = PermissionChecker.check(ctx)
        val battery = BatteryAnalyzer.analyze(ctx)
        val memory = SystemAnalyzer.memory(ctx)
        val storage = SystemAnalyzer.storage()
        val system = SystemAnalyzer.system(ctx)

        val apps = AppAnalyzer(ctx)
        val usage = if (perms.usageAccess) apps.usageLastDay() else emptyList()
        val caches = if (perms.usageAccess) apps.storage() else null
        val sizes = caches?.associate { it.packageName to it.totalBytes }.orEmpty()
        val unused = if (perms.usageAccess) apps.unused(30, sizes) else emptyList()
        val exempt = apps.batteryExempt()
        val crashes = if (perms.readLogs && perms.usageAccess) {
            runCatching { CrashAnalyzer.analyze(ctx, apps::label) }.getOrNull()
        } else null

        val findings = DiagnosticEngine.run(
            DiagnosticEngine.Input(
                battery, memory, storage, system, perms, usage, exempt, caches, unused, crashes
            )
        )

        return Report(
            timestamp = System.currentTimeMillis(),
            battery = battery,
            memory = memory,
            storage = storage,
            system = system,
            permissions = perms,
            topUsage = usage,
            batteryExempt = exempt,
            caches = caches?.sortedByDescending { it.cacheBytes }?.take(20).orEmpty(),
            totalCacheBytes = caches?.sumOf { it.cacheBytes },
            unused = unused,
            crashes = crashes,
            userAppCount = apps.userApps.size,
            findings = findings,
            score = DiagnosticEngine.score(findings)
        )
    }
}
