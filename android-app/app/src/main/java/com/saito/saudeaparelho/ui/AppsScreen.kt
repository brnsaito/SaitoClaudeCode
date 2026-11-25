package com.saito.saudeaparelho.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saito.saudeaparelho.data.Format
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report

@Composable
fun AppsScreen(r: Report, onFix: (Fix) -> Unit) {
    ScreenColumn {
        if (!r.permissions.usageAccess) {
            SectionCard("Permissão necessária") {
                EmptyNote("Permita o \"Acesso ao uso\" para analisar caches, apps sem uso e consumo por app.")
                TextButton(onClick = { onFix(Fix("", FixAction.GRANT_USAGE_ACCESS)) }) { Text("Permitir acesso ao uso") }
            }
            return@ScreenColumn
        }

        SectionCard("Cache dos apps" + (r.totalCacheBytes?.let { " · ${Format.bytes(it)}" } ?: "")) {
            EmptyNote("Cache são arquivos temporários; limpar é seguro e libera espaço (os apps recriam o necessário).")
            TextButton(onClick = { onFix(Fix("", FixAction.CLEAR_ALL_CACHE)) }) { Text("Limpar cache de todos os apps") }
            r.caches.filter { it.cacheBytes > 1024 * 1024 }.take(12).forEach { a ->
                AppRow(a.label, "Cache ${Format.bytes(a.cacheBytes)} · Total ${Format.bytes(a.totalBytes)}", "Limpar") {
                    onFix(Fix("", FixAction.OPEN_APP_DETAILS, a.packageName))
                }
            }
        }

        SectionCard("Sem uso há 30+ dias (${r.unused.size})") {
            if (r.unused.isEmpty()) EmptyNote("Nenhum app esquecido. 👌")
            else EmptyNote("Apps parados ocupam espaço e alguns ainda rodam em segundo plano. Considere desinstalar.")
            r.unused.take(25).forEach { u ->
                AppRow(
                    u.label,
                    "Último uso: ${Format.daysAgo(u.lastUsed)}" + (u.sizeBytes?.let { " · ${Format.bytes(it)}" } ?: ""),
                    "Desinstalar"
                ) { onFix(Fix("", FixAction.UNINSTALL_APP, u.packageName)) }
            }
        }

        SectionCard("Mais usados no último dia") {
            if (r.topUsage.isEmpty()) EmptyNote("Sem dados ainda.")
            r.topUsage.forEach { u ->
                AppRow(u.label, "Tela ${Format.duration(u.foregroundMs)}" +
                    if (u.fgServiceMs > 60_000) " · 2º plano ${Format.duration(u.fgServiceMs)}" else "", "Detalhes") {
                    onFix(Fix("", FixAction.OPEN_APP_DETAILS, u.packageName))
                }
            }
        }
    }
}
