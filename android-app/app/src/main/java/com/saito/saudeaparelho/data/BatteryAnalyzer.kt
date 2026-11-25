package com.saito.saudeaparelho.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.saito.saudeaparelho.model.BatteryInfo
import kotlin.math.abs

object BatteryAnalyzer {

    fun analyze(ctx: Context): BatteryInfo {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = ctx.getSystemService(BatteryManager::class.java)

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = (intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
        val pct = if (level >= 0) level * 100 / scale else 0
        val statusCode = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val healthCode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty().ifBlank { "—" }
        val cycles = if (Build.VERSION.SDK_INT >= 34) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 }
        } else null

        val charging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0

        val currentMa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { if (abs(it) > 20_000) it / 1000 else it } // µA na maioria; alguns aparelhos já reportam mA
            ?.let { abs(it) }

        val counterMah = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it != Int.MIN_VALUE && it > 0 }
            ?.let { if (it > 50_000) it / 1000 else it }

        val estimatedFull = if (counterMah != null && pct >= 15) counterMah * 100 / pct else null
        val design = designCapacity(ctx)
        val healthPct = if (estimatedFull != null && design != null) {
            (estimatedFull * 100 / design).coerceIn(1, 120)
        } else null

        val drain = BatteryHistory.drain(BatteryHistory.load(ctx))

        val chargeRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && charging) {
            bm?.computeChargeTimeRemaining()?.takeIf { it > 0 }?.let { it / 60_000 }
        } else null

        return BatteryInfo(
            levelPercent = pct,
            isCharging = charging,
            plugType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Tomada"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Sem fio"
                0 -> "Desconectado"
                else -> "Outro"
            },
            status = when (statusCode) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
                BatteryManager.BATTERY_STATUS_FULL -> "Completa"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não está carregando"
                else -> "Desconhecido"
            },
            health = healthLabel(healthCode),
            healthCode = healthCode,
            temperatureC = temp,
            voltageMv = voltage,
            technology = tech,
            cycleCount = cycles,
            currentNowMa = currentMa,
            chargeCounterMah = counterMah,
            estimatedFullMah = estimatedFull,
            designCapacityMah = design,
            healthPercentEstimate = healthPct,
            drainPerHour = drain.overall,
            idleDrainPerHour = drain.idle,
            historyHours = drain.coveredHours,
            chargeTimeRemainingMin = chargeRemaining
        )
    }

    fun healthLabel(code: Int) = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Esgotada"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Falha"
        BatteryManager.BATTERY_HEALTH_COLD -> "Muito fria"
        else -> "Desconhecida"
    }

    /** Capacidade de fábrica (mAh) via perfil de energia do sistema. Nem todo aparelho expõe. */
    private fun designCapacity(ctx: Context): Int? = try {
        val cls = Class.forName("com.android.internal.os.PowerProfile")
        val profile = cls.getConstructor(Context::class.java).newInstance(ctx)
        val cap = cls.getMethod("getBatteryCapacity").invoke(profile) as Double
        cap.toInt().takeIf { it in 800..30_000 }
    } catch (_: Throwable) {
        null
    }
}
