package com.saito.saudeaparelho.ui

import androidx.compose.runtime.Composable
import com.saito.saudeaparelho.data.Format
import com.saito.saudeaparelho.model.Category
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report

@Composable
fun BatteryScreen(r: Report, onFix: (Fix) -> Unit) {
    val b = r.battery
    ScreenColumn {
        SectionCard("Estado da bateria") {
            UsageBar("Carga", 1f - b.levelPercent / 100f, "${b.levelPercent}% · ${b.status}")
            InfoRow("Saúde (sistema)", b.health, if (b.health == "Boa") Good else Warn)
            b.healthPercentEstimate?.let {
                InfoRow("Capacidade estimada", "$it% da original", if (it >= 80) Good else if (it >= 65) Warn else Bad)
            }
            b.estimatedFullMah?.let { InfoRow("Carga máxima atual (est.)", "$it mAh") }
            b.designCapacityMah?.let { InfoRow("Capacidade de fábrica", "$it mAh") }
            b.cycleCount?.let { InfoRow("Ciclos de carga", "$it") }
            InfoRow("Temperatura", "${Format.dec(b.temperatureC)} °C",
                if (b.temperatureC >= 45) Bad else if (b.temperatureC >= 40) Warn else Good)
            InfoRow("Tensão", "${b.voltageMv} mV")
            b.currentNowMa?.let { InfoRow(if (b.isCharging) "Corrente de carga" else "Consumo agora", "$it mA") }
            InfoRow("Fonte", b.plugType)
            InfoRow("Tecnologia", b.technology)
            b.chargeTimeRemainingMin?.let { InfoRow("Tempo até carga completa", Format.duration(it * 60_000)) }
        }

        SectionCard("Velocidade de descarga") {
            if (b.drainPerHour == null) {
                EmptyNote(
                    "Coletando dados… O app mede o nível da bateria a cada ~15 min em segundo plano. " +
                        "Use o aparelho fora do carregador por algumas horas para ver a média real."
                )
            } else {
                InfoRow("Média geral", "${Format.dec(b.drainPerHour)}%/h",
                    if (b.drainPerHour >= 8) Bad else if (b.drainPerHour >= 5) Warn else Good)
                b.idleDrainPerHour?.let {
                    InfoRow("Com a tela apagada", "${Format.dec(it)}%/h", if (it >= 2) Bad else if (it >= 1) Warn else Good)
                }
                InfoRow("Autonomia estimada", Format.duration((100f / b.drainPerHour * 3_600_000).toLong()))
                EmptyNote("Base: ${Format.dec(b.historyHours)} h de uso fora do carregador nas últimas 48 h.")
            }
        }

        SectionCard("Apps que mais consumiram (último dia)") {
            if (!r.permissions.usageAccess) {
                EmptyNote("Permita o acesso ao uso para ver esta lista.")
                androidx.compose.material3.TextButton(onClick = { onFix(Fix("", FixAction.GRANT_USAGE_ACCESS)) }) {
                    androidx.compose.material3.Text("Permitir acesso ao uso")
                }
            } else if (r.topUsage.isEmpty()) EmptyNote("Sem dados de uso ainda.")
            r.topUsage.take(10).forEach { u ->
                AppRow(
                    u.label,
                    "Tela: ${Format.duration(u.foregroundMs)}" +
                        if (u.fgServiceMs > 60_000) " · Segundo plano: ${Format.duration(u.fgServiceMs)}" else "",
                    "Ajustar"
                ) { onFix(Fix("", FixAction.OPEN_APP_DETAILS, u.packageName)) }
            }
            if (r.topUsage.isNotEmpty()) EmptyNote(
                "O Android não informa o consumo exato por app a terceiros; o tempo de tela e de " +
                    "serviço em segundo plano é o melhor indicador disponível."
            )
        }

        SectionCard("Liberados da otimização de bateria (${r.batteryExempt.size})") {
            if (r.batteryExempt.isEmpty()) EmptyNote("Nenhum app do usuário está liberado. Ótimo!")
            r.batteryExempt.take(15).forEach { a ->
                AppRow(a.label, "Pode rodar sem restrição em segundo plano", "Ajustar") {
                    onFix(Fix("", FixAction.OPEN_APP_DETAILS, a.packageName))
                }
            }
        }

        FindingsSection(r, Category.BATTERY, onFix)
    }
}

@Composable
fun FindingsSection(r: Report, category: Category, onFix: (Fix) -> Unit) {
    val list = r.findings.filter { it.category == category }
    if (list.isEmpty()) return
    SectionCard("Diagnóstico") {
        list.forEach { FindingCard(it) { f -> f.fix?.let(onFix) } }
    }
}
