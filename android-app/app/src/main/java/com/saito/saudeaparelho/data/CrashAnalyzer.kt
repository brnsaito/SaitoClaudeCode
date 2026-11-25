package com.saito.saudeaparelho.data

import android.content.Context
import android.os.DropBoxManager
import com.saito.saudeaparelho.model.CrashRecord
import com.saito.saudeaparelho.model.CrashReport

/**
 * Lê o registro de falhas do sistema (DropBox): travamentos (crash), "app não responde" (ANR)
 * e falhas nativas dos últimos dias. Requer READ_LOGS (concedida via ADB) + acesso de uso.
 */
object CrashAnalyzer {

    private enum class Kind { CRASH, ANR, NATIVE, SYSTEM }

    private val TAGS = mapOf(
        "data_app_crash" to Kind.CRASH,
        "system_app_crash" to Kind.CRASH,
        "data_app_anr" to Kind.ANR,
        "system_app_anr" to Kind.ANR,
        "data_app_native_crash" to Kind.NATIVE,
        "system_app_native_crash" to Kind.NATIVE,
        "SYSTEM_TOMBSTONE" to Kind.NATIVE,
        "system_server_crash" to Kind.SYSTEM,
        "system_server_anr" to Kind.SYSTEM,
        "system_server_watchdog" to Kind.SYSTEM,
        "system_server_native_crash" to Kind.SYSTEM,
        "SYSTEM_LAST_KMSG" to Kind.SYSTEM
    )

    private val PKG_PATTERNS = listOf(
        Regex("^Package: ([\\w.]+)", RegexOption.MULTILINE),
        Regex("^Process: ([\\w.]+)", RegexOption.MULTILINE),
        Regex(">>> ([\\w.]+)(?::[\\w.]+)? <<<"),
        Regex("^Cmdline: ([\\w.]+)", RegexOption.MULTILINE)
    )

    fun analyze(ctx: Context, labeler: (String) -> String, days: Int = 7): CrashReport? {
        val dbm = ctx.getSystemService(DropBoxManager::class.java) ?: return null
        val since = System.currentTimeMillis() - days * 24L * 3600 * 1000

        class Acc(var crash: Int = 0, var anr: Int = 0, var native: Int = 0, var last: Long = 0)
        val perPkg = HashMap<String, Acc>()
        var systemEvents = 0

        try {
            for ((tag, kind) in TAGS) {
                var t = since
                var guard = 0
                while (guard++ < 3000) {
                    val entry = dbm.getNextEntry(tag, t) ?: break
                    t = entry.timeMillis
                    if (kind == Kind.SYSTEM) {
                        systemEvents++
                        entry.close()
                        continue
                    }
                    val text = entry.getText(4096).orEmpty()
                    entry.close()
                    val pkg = PKG_PATTERNS.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1) }
                        ?.takeIf { it.contains('.') } ?: continue
                    val acc = perPkg.getOrPut(pkg) { Acc() }
                    when (kind) {
                        Kind.CRASH -> acc.crash++
                        Kind.ANR -> acc.anr++
                        Kind.NATIVE -> acc.native++
                        Kind.SYSTEM -> Unit
                    }
                    if (t > acc.last) acc.last = t
                }
            }
        } catch (_: SecurityException) {
            return null
        }

        val records = perPkg.map { (pkg, a) ->
            CrashRecord(pkg, labeler(pkg), a.crash, a.anr, a.native, a.last)
        }.sortedByDescending { it.total }
        return CrashReport(records, systemEvents, days)
    }
}
