package com.saito.saudeaparelho.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import com.saito.saudeaparelho.model.PermissionState

object PermissionChecker {

    fun check(ctx: Context): PermissionState = PermissionState(
        usageAccess = hasUsageAccess(ctx),
        writeSettings = Settings.System.canWrite(ctx),
        writeSecureSettings = granted(ctx, Manifest.permission.WRITE_SECURE_SETTINGS),
        readLogs = granted(ctx, Manifest.permission.READ_LOGS),
        allFilesAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
    )

    fun hasUsageAccess(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            granted(ctx, Manifest.permission.PACKAGE_USAGE_STATS)
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun granted(ctx: Context, perm: String) =
        ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
}
