package com.saito.saudeaparelho.data

import java.util.Locale

object Format {
    private val BR = Locale("pt", "BR")

    fun bytes(b: Long): String {
        if (b < 1024) return "$b B"
        val units = listOf("KB", "MB", "GB", "TB")
        var v = b / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format(BR, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
    }

    fun duration(ms: Long): String {
        val min = ms / 60_000
        val d = min / (60 * 24)
        val h = (min / 60) % 24
        val m = min % 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}min"
            else -> "${m}min"
        }
    }

    fun pct(f: Float) = "${(f * 100).toInt()}%"

    fun dec(v: Float, digits: Int = 1) = String.format(BR, "%.${digits}f", v)

    fun daysAgo(time: Long?): String {
        if (time == null || time <= 0) return "nunca registrado"
        val d = (System.currentTimeMillis() - time) / (24L * 3600 * 1000)
        return if (d <= 0) "hoje" else "há $d dias"
    }
}
