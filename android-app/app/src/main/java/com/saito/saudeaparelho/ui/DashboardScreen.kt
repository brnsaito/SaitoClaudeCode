package com.saito.saudeaparelho.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saito.saudeaparelho.data.Format
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report
import com.saito.saudeaparelho.model.Severity

@Composable
fun DashboardScreen(r: Report, onFix: (Fix) -> Unit, onGoOptimize: () -> Unit) {
    ScreenColumn {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScoreGauge(r.score) }
        Text(
            "${r.system.manufacturer} ${r.system.model} · Android ${r.system.androidVersion}",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Bateria", "${r.battery.levelPercent}%", Modifier.weight(1f))
            StatTile("Temp.", "${Format.dec(r.battery.temperatureC)}°", Modifier.weight(1f))
            StatTile("RAM livre", Format.pct(r.memory.freeFraction), Modifier.weight(1f))
            StatTile("Espaço", Format.pct(r.storage.freeFraction), Modifier.weight(1f))
        }

        Button(onClick = onGoOptimize, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.AutoFixHigh, null)
            Text("  Otimizar agora")
        }

        PermissionsCard(r, onFix)

        val problems = r.findings.filter { it.severity != Severity.INFO }
        val tips = r.findings.filter { it.severity == Severity.INFO }
        SectionCard("Problemas encontrados (${problems.size})") {
            if (problems.isEmpty()) EmptyNote("Nenhum problema relevante. Seu aparelho está em boa forma! 🎉")
            problems.forEach { FindingCard(it) { f -> f.fix?.let(onFix) } }
        }
        if (tips.isNotEmpty()) SectionCard("Dicas de otimização (${tips.size})") {
            tips.forEach { FindingCard(it) { f -> f.fix?.let(onFix) } }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionsCard(r: Report, onFix: (Fix) -> Unit) {
    val p = r.permissions
    if (p.usageAccess && p.writeSettings) return
    SectionCard("Ative a análise completa") {
        EmptyNote("Sem essas permissões o app não consegue ver quais apps drenam bateria ou aplicar ajustes.")
        if (!p.usageAccess) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Acesso ao uso de apps", Modifier.weight(1f))
            TextButton(onClick = { onFix(Fix("", FixAction.GRANT_USAGE_ACCESS)) }) { Text("Permitir") }
        }
        if (!p.writeSettings) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Modificar configurações do sistema", Modifier.weight(1f))
            TextButton(onClick = { onFix(Fix("", FixAction.GRANT_WRITE_SETTINGS)) }) { Text("Permitir") }
        }
    }
}
