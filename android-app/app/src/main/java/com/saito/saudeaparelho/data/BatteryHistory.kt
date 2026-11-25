package com.saito.saudeaparelho.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import java.io.File

data class BatterySample(
    val time: Long,
    val level: Int,
    val charging: Boolean,
    val screenOn: Boolean,
    val tempC: Float
)

/**
 * Guarda amostras periódicas do nível da bateria (coletadas pelo WorkManager a cada ~15 min)
 * para calcular a velocidade real de descarga.
 */
object BatteryHistory {
    private const val FILE = "battery_samples.csv"
    private const val MAX_AGE_MS = 7L * 24 * 3600 * 1000
    private const val MIN_GAP_MS = 5L * 60 * 1000
    private const val MAX_GAP_MS = 3L * 3600 * 1000

    @Synchronized
    fun record(ctx: Context) {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level < 0) return
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val screenOn = ctx.getSystemService(PowerManager::class.java)?.isInteractive ?: true
        val now = System.currentTimeMillis()

        val samples = load(ctx).filter { now - it.time < MAX_AGE_MS }.toMutableList()
        val last = samples.lastOrNull()
        // Evita amostras duplicadas muito próximas (ex.: abrir o app várias vezes)
        if (last != null && now - last.time < 60_000) return
        samples += BatterySample(now, level * 100 / scale, charging, screenOn, temp)
        file(ctx).writeText(samples.joinToString("\n") {
            "${it.time},${it.level},${if (it.charging) 1 else 0},${if (it.screenOn) 1 else 0},${it.tempC}"
        })
    }

    @Synchronized
    fun load(ctx: Context): List<BatterySample> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull { line ->
            val p = line.split(',')
            if (p.size < 5) return@mapNotNull null
            runCatching {
                BatterySample(p[0].toLong(), p[1].toInt(), p[2] == "1", p[3] == "1", p[4].toFloat())
            }.getOrNull()
        }.sortedBy { it.time }
    }

    data class DrainStats(val overall: Float?, val idle: Float?, val coveredHours: Float)

    /** Calcula %/hora de descarga considerando apenas intervalos sem carregador nas últimas 48h. */
    fun drain(samples: List<BatterySample>, now: Long = System.currentTimeMillis()): DrainStats {
        val recent = samples.filter { now - it.time < 48L * 3600 * 1000 }
        var dropAll = 0f; var hoursAll = 0f
        var dropIdle = 0f; var hoursIdle = 0f
        for (i in 1 until recent.size) {
            val a = recent[i - 1]; val b = recent[i]
            val dt = b.time - a.time
            if (dt < MIN_GAP_MS || dt > MAX_GAP_MS) continue
            if (a.charging || b.charging || b.level > a.level) continue
            val h = dt / 3_600_000f
            val drop = (a.level - b.level).toFloat()
            dropAll += drop; hoursAll += h
            if (!a.screenOn && !b.screenOn) { dropIdle += drop; hoursIdle += h }
        }
        return DrainStats(
            overall = if (hoursAll >= 1f) dropAll / hoursAll else null,
            idle = if (hoursIdle >= 1f) dropIdle / hoursIdle else null,
            coveredHours = hoursAll
        )
    }

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)
}
