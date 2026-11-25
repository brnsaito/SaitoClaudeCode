package com.saito.saudeaparelho.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.saito.saudeaparelho.OptimizeOptions
import com.saito.saudeaparelho.UiState
import com.saito.saudeaparelho.data.Optimizer
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report

@Composable
fun OptimizeScreen(r: Report, ui: UiState, onFix: (Fix) -> Unit, onOptimize: (OptimizeOptions) -> Unit) {
    var kill by rememberSaveable { mutableStateOf(true) }
    var cache by rememberSaveable { mutableStateOf(true) }
    var timeout by rememberSaveable { mutableStateOf(true) }
    var bright by rememberSaveable { mutableStateOf(true) }
    var anim by rememberSaveable { mutableStateOf(r.permissions.writeSecureSettings) }

    ScreenColumn {
        SectionCard("Otimização em 1 toque") {
            EmptyNote("Escolha o que aplicar. Nada é desinstalado ou apagado sem sua confirmação.")
            CheckRow("Encerrar processos em segundo plano", kill) { kill = it }
            CheckRow("Limpar arquivos temporários deste app", cache) { cache = it }
            CheckRow("Tela apaga após 30s" + needs(r.permissions.writeSettings), timeout) { timeout = it }
            CheckRow("Brilho automático" + needs(r.permissions.writeSettings), bright) { bright = it }
            CheckRow("Animações 0,5x" + if (r.permissions.writeSecureSettings) "" else " (requer ADB)", anim) { anim = it }
            Button(
                onClick = { onOptimize(OptimizeOptions(kill, cache, timeout, bright, anim)) },
                enabled = !ui.optimizing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.optimizing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Otimizar")
            }
            ui.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }

        SectionCard("Correções rápidas") {
            QuickAction("Liberar memória RAM", "Encerra apps parados em segundo plano") { onFix(Fix("", FixAction.KILL_BACKGROUND)) }
            QuickAction("Limpar cache de todos os apps", "Assistente do sistema (Android 11+)") { onFix(Fix("", FixAction.CLEAR_ALL_CACHE)) }
            QuickAction("Gerenciar armazenamento", "Arquivos grandes, duplicados e apps") { onFix(Fix("", FixAction.OPEN_STORAGE)) }
            QuickAction("Economia de bateria", "Ativar ou programar") { onFix(Fix("", FixAction.OPEN_BATTERY_SAVER)) }
            QuickAction("Apps sem restrição de bateria", "Revise quem pode rodar em segundo plano") { onFix(Fix("", FixAction.OPEN_BATTERY_OPTIMIZATION)) }
            QuickAction("Tela e brilho", "Brilho, tempo de tela, taxa de atualização") { onFix(Fix("", FixAction.OPEN_DISPLAY)) }
            QuickAction("Tema escuro", "Economiza bateria em telas OLED") { onFix(Fix("", FixAction.OPEN_DARK_THEME)) }
            QuickAction("Atualização do sistema", "Correções de bugs e segurança") { onFix(Fix("", FixAction.OPEN_SYSTEM_UPDATE)) }
        }

        SectionCard("Permissões") {
            PermRow("Acesso ao uso de apps", r.permissions.usageAccess) { onFix(Fix("", FixAction.GRANT_USAGE_ACCESS)) }
            PermRow("Modificar configurações do sistema", r.permissions.writeSettings) { onFix(Fix("", FixAction.GRANT_WRITE_SETTINGS)) }
            PermRow("Gerenciar arquivos (limpeza de cache)", r.permissions.allFilesAccess) { onFix(Fix("", FixAction.GRANT_ALL_FILES)) }
            PermRow("Configurações seguras (ADB)", r.permissions.writeSecureSettings, null)
            PermRow("Registro de travamentos (ADB)", r.permissions.readLogs, null)
        }

        SectionCard("Recursos avançados (ADB)") {
            EmptyNote(
                "Animações, \"Não manter atividades\" e o histórico de travamentos exigem permissões que só " +
                    "podem ser concedidas pelo computador, uma única vez:\n" +
                    "1. Ative Opções do desenvolvedor (toque 7x em \"Número da versão\")\n" +
                    "2. Ative a Depuração USB e conecte ao computador\n" +
                    "3. Rode os comandos abaixo (Android Platform Tools):"
            )
            SelectionContainer {
                Text(Optimizer.ADB_GRANT, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { onFix(Fix("", FixAction.OPEN_DEVELOPER_OPTIONS)) }) { Text("Abrir opções do desenvolvedor") }
        }
    }
}

private fun needs(ok: Boolean) = if (ok) "" else " (requer permissão)"

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, onClick: () -> Unit) {
    AppRow(title, subtitle, "Abrir", onClick)
}

@Composable
private fun PermRow(label: String, granted: Boolean, onGrant: (() -> Unit)?) {
    AppRow(
        label,
        if (granted) "Concedida" else "Não concedida",
        if (!granted && onGrant != null) "Permitir" else null
    ) { onGrant?.invoke() }
}
