package com.saito.saudeaparelho.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.saito.saudeaparelho.data.Format
import com.saito.saudeaparelho.data.Optimizer
import com.saito.saudeaparelho.model.Category
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report

@Composable
fun PerformanceScreen(r: Report, onFix: (Fix) -> Unit) {
    val s = r.system
    val m = r.memory
    val st = r.storage
    ScreenColumn {
        SectionCard("Recursos") {
            UsageBar("Memória RAM", 1f - m.freeFraction,
                "${Format.bytes(m.totalBytes - m.availBytes)} de ${Format.bytes(m.totalBytes)}")
            UsageBar("Armazenamento", 1f - st.freeFraction,
                "${Format.bytes(st.totalBytes - st.availBytes)} de ${Format.bytes(st.totalBytes)}")
            InfoRow("Temperatura do sistema", s.thermalLabel,
                if (s.thermalStatus >= 3) Bad else if (s.thermalStatus >= 2) Warn else Good)
            s.thermalHeadroom?.let {
                InfoRow("Margem térmica", "${(100 - it * 100).toInt().coerceIn(0, 100)}% livre",
                    if (it >= 0.9f) Bad else if (it >= 0.7f) Warn else Good)
            }
            InfoRow("Processador", "${s.cpuCores} núcleos" + (s.cpuMaxFreqMhz?.let { " · até $it MHz" } ?: ""))
            InfoRow("Ligado há", Format.duration(s.uptimeMs),
                if (s.uptimeMs > 7 * 86_400_000L) Warn else Good)
            InfoRow("Animações", "${Format.dec(s.animatorScale)}x")
            InfoRow("Economia de energia", if (s.powerSaveMode) "Ativa" else "Desativada")
            InfoRow("Apps instalados (usuário)", "${r.userAppCount}")
            InfoRow("Patch de segurança", s.securityPatch)
        }

        SectionCard("Travamentos e \"app não responde\"") {
            val c = r.crashes
            if (c == null) {
                EmptyNote(
                    "Para ler o histórico de travamentos do sistema, o Android exige uma permissão " +
                        "concedida uma única vez pelo computador (ADB). Com o celular conectado e a " +
                        "Depuração USB ativa, rode:"
                )
                Text(Optimizer.ADB_GRANT, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                EmptyNote("Também é necessário o \"Acesso ao uso\". Depois, reabra o app.")
            } else if (c.apps.isEmpty() && c.systemEvents == 0) {
                EmptyNote("Nenhum travamento registrado nos últimos ${c.days} dias. 👍")
            } else {
                if (c.systemEvents > 0) InfoRow("Falhas do sistema", "${c.systemEvents}", Bad)
                c.apps.take(15).forEach { a ->
                    AppRow(
                        a.label,
                        listOfNotNull(
                            a.crashes.takeIf { it > 0 }?.let { "$it fechamento(s)" },
                            a.anrs.takeIf { it > 0 }?.let { "$it não responde" },
                            a.nativeCrashes.takeIf { it > 0 }?.let { "$it nativa(s)" }
                        ).joinToString(" · ") + " · último ${Format.daysAgo(a.lastTime)}",
                        "Corrigir"
                    ) { onFix(Fix("", FixAction.OPEN_APP_DETAILS, a.packageName)) }
                }
            }
        }

        SectionCard("Causas comuns de lentidão") {
            EmptyNote(
                "• Armazenamento acima de 85% cheio\n" +
                    "• Pouca RAM livre por excesso de apps abertos\n" +
                    "• Aquecimento (o processador reduz a velocidade)\n" +
                    "• Muitos dias sem reiniciar\n" +
                    "• Apps com defeito travando repetidamente\n" +
                    "• Bateria desgastada (o sistema limita o desempenho)\n" +
                    "• Sistema e apps desatualizados"
            )
        }

        FindingsSection(r, Category.PERFORMANCE, onFix)
        FindingsSection(r, Category.STABILITY, onFix)
        FindingsSection(r, Category.STORAGE, onFix)
        FindingsSection(r, Category.SECURITY, onFix)
    }
}
