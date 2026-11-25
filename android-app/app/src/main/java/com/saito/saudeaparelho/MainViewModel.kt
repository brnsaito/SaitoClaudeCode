package com.saito.saudeaparelho

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saito.saudeaparelho.data.Optimizer
import com.saito.saudeaparelho.data.ReportBuilder
import com.saito.saudeaparelho.model.ActionResult
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = false,
    val report: Report? = null,
    val optimizing: Boolean = false,
    val log: List<String> = emptyList()
)

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Launch(val intent: Intent, val forResult: Boolean) : UiEvent
}

/** Opções da otimização em 1 toque. */
data class OptimizeOptions(
    val killBackground: Boolean = true,
    val clearOwnCache: Boolean = true,
    val screenTimeout: Boolean = true,
    val autoBrightness: Boolean = true,
    val animations: Boolean = true
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { ReportBuilder.build(getApplication()) }.getOrNull()
            }
            _state.update { it.copy(loading = false, report = r ?: it.report) }
            if (r == null) _events.send(UiEvent.Message("Não foi possível concluir a análise."))
        }
    }

    fun perform(fix: Fix) {
        viewModelScope.launch {
            val res: ActionResult = withContext(Dispatchers.IO) { Optimizer.perform(getApplication(), fix) }
            res.message?.let { _events.send(UiEvent.Message(it)) }
            res.intent?.let { _events.send(UiEvent.Launch(it, res.forResult)) }
            if (res.intent == null) refresh()
        }
    }

    fun optimizeAll(opt: OptimizeOptions) {
        if (_state.value.optimizing) return
        _state.update { it.copy(optimizing = true, log = emptyList()) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val report = _state.value.report
            val sys = report?.system
            val perms = report?.permissions
            val log = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                fun run(fix: FixAction) {
                    val r = Optimizer.perform(ctx, Fix("", fix))
                    r.message?.let { log += "✓ $it" }
                }
                if (opt.killBackground) run(FixAction.KILL_BACKGROUND)
                if (opt.clearOwnCache) run(FixAction.CLEAR_OWN_CACHE)
                if (perms?.writeSettings == true) {
                    if (opt.screenTimeout && (sys?.screenTimeoutMs ?: 0) > 60_000) run(FixAction.SET_SCREEN_TIMEOUT)
                    if (opt.autoBrightness && sys?.autoBrightness == false) run(FixAction.SET_AUTO_BRIGHTNESS)
                } else if (opt.screenTimeout || opt.autoBrightness) {
                    log += "• Ajustes de tela pulados: permita \"Modificar configurações do sistema\"."
                }
                if (opt.animations) {
                    if (perms?.writeSecureSettings == true) {
                        if ((sys?.animatorScale ?: 0f) >= 1f) run(FixAction.REDUCE_ANIMATIONS)
                    } else log += "• Animações puladas: requer permissão via ADB (Recursos avançados)."
                }
            }
            val pending = report?.findings?.count { it.fix != null && it.fix.action.name.startsWith("OPEN") } ?: 0
            if (pending > 0) log += "• $pending item(ns) precisam da sua confirmação — veja \"Problemas encontrados\" no Início."
            _state.update { it.copy(optimizing = false, log = log) }
            _events.send(UiEvent.Message("Otimização concluída"))
            refresh()
        }
    }

    fun clearLog() = _state.update { it.copy(log = emptyList()) }
}
