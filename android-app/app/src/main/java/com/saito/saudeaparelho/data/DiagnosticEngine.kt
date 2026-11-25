package com.saito.saudeaparelho.data

import android.os.BatteryManager
import com.saito.saudeaparelho.model.AppStorage
import com.saito.saudeaparelho.model.AppUsage
import com.saito.saudeaparelho.model.AppRef
import com.saito.saudeaparelho.model.BatteryInfo
import com.saito.saudeaparelho.model.Category
import com.saito.saudeaparelho.model.CrashReport
import com.saito.saudeaparelho.model.Finding
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction
import com.saito.saudeaparelho.model.MemoryStats
import com.saito.saudeaparelho.model.PermissionState
import com.saito.saudeaparelho.model.Severity
import com.saito.saudeaparelho.model.StorageStats
import com.saito.saudeaparelho.model.SystemInfo
import com.saito.saudeaparelho.model.UnusedApp

/** Regras que transformam as medições em problemas identificados + correção sugerida. */
object DiagnosticEngine {

    private const val GB = 1024L * 1024 * 1024
    private const val HOUR = 3_600_000L

    data class Input(
        val battery: BatteryInfo,
        val memory: MemoryStats,
        val storage: StorageStats,
        val system: SystemInfo,
        val permissions: PermissionState,
        val usage: List<AppUsage>,
        val batteryExempt: List<AppRef>,
        val caches: List<AppStorage>?,
        val unused: List<UnusedApp>,
        val crashes: CrashReport?
    )

    fun run(i: Input): List<Finding> {
        val f = mutableListOf<Finding>()
        battery(i, f)
        performance(i, f)
        storage(i, f)
        stability(i, f)
        security(i, f)
        permissions(i, f)
        return f.sortedByDescending { it.severity.ordinal }
    }

    fun score(findings: List<Finding>): Int {
        // Dicas (INFO) pesam pouco e têm teto, para não "punir" preferências do usuário
        val info = findings.filter { it.severity == Severity.INFO }.sumOf { it.severity.weight }.coerceAtMost(10)
        val rest = findings.filter { it.severity != Severity.INFO }.sumOf { it.severity.weight }
        return (100 - info - rest).coerceIn(0, 100)
    }

    private fun battery(i: Input, f: MutableList<Finding>) {
        val b = i.battery
        val s = i.system
        val badHealth = b.healthCode != BatteryManager.BATTERY_HEALTH_GOOD &&
            b.healthCode != BatteryManager.BATTERY_HEALTH_UNKNOWN
        if (badHealth) {
            f += Finding(
                "bat_health", Category.BATTERY, Severity.CRITICAL,
                "Sistema reporta bateria: ${b.health}",
                "O próprio Android sinaliza problema na bateria. Isso pode causar desligamentos, " +
                    "travamentos e aquecimento. Procure uma assistência técnica autorizada."
            )
        }
        b.healthPercentEstimate?.let { h ->
            if (h < 80) f += Finding(
                "bat_capacity", Category.BATTERY, if (h < 65) Severity.CRITICAL else Severity.WARNING,
                "Capacidade estimada em $h% da original",
                "Carga máxima estimada ~${b.estimatedFullMah} mAh de ${b.designCapacityMah} mAh de fábrica. " +
                    "Abaixo de 80% a bateria dura menos e o aparelho pode reduzir o desempenho para evitar " +
                    "desligamentos. Estimativa aproximada: confira com a carga acima de 50%."
            )
        }
        b.cycleCount?.let { c ->
            if (c >= 500) f += Finding(
                "bat_cycles", Category.BATTERY, if (c >= 800) Severity.WARNING else Severity.INFO,
                "$c ciclos de carga",
                "Baterias de lítio costumam perder capacidade perceptível após 500–800 ciclos."
            )
        }
        when {
            b.temperatureC >= 45f -> f += Finding(
                "bat_temp", Category.BATTERY, Severity.CRITICAL,
                "Bateria muito quente (${Format.dec(b.temperatureC)} °C)",
                "Calor acelera o desgaste e faz o processador reduzir a velocidade (travamentos). " +
                    "Tire da capa, evite usar carregando e feche apps pesados.",
                Fix("Encerrar apps em segundo plano", FixAction.KILL_BACKGROUND)
            )
            b.temperatureC >= 40f -> f += Finding(
                "bat_temp", Category.BATTERY, Severity.WARNING,
                "Bateria aquecida (${Format.dec(b.temperatureC)} °C)",
                "Temperatura acima do ideal (até ~35 °C). Pode causar lentidão e drenar mais bateria.",
                Fix("Encerrar apps em segundo plano", FixAction.KILL_BACKGROUND)
            )
        }
        b.drainPerHour?.let { d ->
            if (d >= 8f) f += Finding(
                "bat_drain", Category.BATTERY, if (d >= 15f) Severity.CRITICAL else Severity.WARNING,
                "Descarga rápida: ${Format.dec(d)}%/hora",
                "Média medida nas últimas horas fora do carregador. Veja abaixo os apps que mais " +
                    "rodaram e reduza brilho/tempo de tela.",
                Fix("Abrir economia de bateria", FixAction.OPEN_BATTERY_SAVER)
            )
        }
        b.idleDrainPerHour?.let { d ->
            if (d >= 2f) f += Finding(
                "bat_idle", Category.BATTERY, if (d >= 4f) Severity.CRITICAL else Severity.WARNING,
                "Consumo alto com a tela apagada: ${Format.dec(d)}%/hora",
                "Em repouso o normal é menos de 1%/h. Algum app está mantendo o aparelho acordado — " +
                    "restrinja a atividade em segundo plano dos apps suspeitos.",
                Fix("Otimização de bateria por app", FixAction.OPEN_BATTERY_OPTIMIZATION)
            )
        }
        i.usage.filter { it.fgServiceMs >= 3 * HOUR }.take(3).forEach { u ->
            f += Finding(
                "bat_bg_${u.packageName}", Category.BATTERY, Severity.WARNING,
                "${u.label} rodou ${Format.duration(u.fgServiceMs)} em segundo plano",
                "Serviço ativo por muito tempo no último dia (GPS, sincronização, música, etc.). " +
                    "Se não for intencional, restrinja o uso de bateria desse app.",
                Fix("Abrir configurações do app", FixAction.OPEN_APP_DETAILS, u.packageName)
            )
        }
        if (i.batteryExempt.size >= 5) f += Finding(
            "bat_exempt", Category.BATTERY, Severity.WARNING,
            "${i.batteryExempt.size} apps ignoram a otimização de bateria",
            "Esses apps podem rodar sem restrição em segundo plano: " +
                i.batteryExempt.take(6).joinToString { it.label } + if (i.batteryExempt.size > 6) "…" else "",
            Fix("Revisar lista", FixAction.OPEN_BATTERY_OPTIMIZATION)
        )
        s.screenTimeoutMs?.let { t ->
            if (t > 60_000) f += Finding(
                "scr_timeout", Category.BATTERY, if (t >= 5 * 60_000) Severity.WARNING else Severity.INFO,
                "Tela apaga só após ${Format.duration(t.toLong())}",
                "A tela é o maior consumidor de bateria. 30 segundos é o recomendado.",
                Fix("Ajustar para 30s", FixAction.SET_SCREEN_TIMEOUT)
            )
        }
        if (s.autoBrightness == false && (s.brightness ?: 0) > 180) f += Finding(
            "scr_bright", Category.BATTERY, Severity.INFO,
            "Brilho alto e manual",
            "Ativar o brilho automático reduz o consumo em ambientes internos.",
            Fix("Ativar brilho automático", FixAction.SET_AUTO_BRIGHTNESS)
        )
        if (!s.darkMode) f += Finding(
            "scr_dark", Category.BATTERY, Severity.INFO,
            "Tema escuro desativado",
            "Em telas OLED/AMOLED o tema escuro economiza bateria de forma significativa.",
            Fix("Abrir tema escuro", FixAction.OPEN_DARK_THEME)
        )
        if (b.levelPercent <= 20 && !b.isCharging && !s.powerSaveMode) f += Finding(
            "bat_low", Category.BATTERY, Severity.WARNING,
            "Bateria baixa (${b.levelPercent}%) sem economia de energia",
            "Ative a economia de bateria para estender a autonomia.",
            Fix("Ativar economia", FixAction.OPEN_BATTERY_SAVER)
        )
        if (s.wifiScanAlways == true) f += Finding(
            "wifi_scan", Category.BATTERY, Severity.INFO,
            "Busca de Wi-Fi sempre ativa",
            "O aparelho procura redes Wi-Fi mesmo com o Wi-Fi desligado para melhorar a localização.",
            Fix("Abrir localização", FixAction.OPEN_LOCATION)
        )
    }

    private fun performance(i: Input, f: MutableList<Finding>) {
        val m = i.memory
        val s = i.system
        val free = m.freeFraction
        if (m.lowMemory || free < 0.10f) f += Finding(
            "ram_low", Category.PERFORMANCE, Severity.CRITICAL,
            "Memória RAM esgotada (${Format.pct(free)} livre)",
            "Com pouca RAM o Android fecha e recarrega apps o tempo todo — causa clássica de travamentos.",
            Fix("Liberar memória", FixAction.KILL_BACKGROUND)
        ) else if (free < 0.20f) f += Finding(
            "ram_low", Category.PERFORMANCE, Severity.WARNING,
            "Pouca RAM livre (${Format.pct(free)})",
            "${Format.bytes(m.availBytes)} livres de ${Format.bytes(m.totalBytes)}.",
            Fix("Liberar memória", FixAction.KILL_BACKGROUND)
        )
        when {
            s.thermalStatus >= 3 -> f += Finding(
                "thermal", Category.PERFORMANCE, Severity.CRITICAL,
                "Aquecimento severo: processador limitado",
                "O sistema está reduzindo a velocidade da CPU/GPU para esfriar (throttling). " +
                    "Isso causa travamentos. Pare jogos/câmera, retire do carregador e da capa.",
                Fix("Encerrar apps em segundo plano", FixAction.KILL_BACKGROUND)
            )
            s.thermalStatus == 2 -> f += Finding(
                "thermal", Category.PERFORMANCE, Severity.WARNING,
                "Aquecimento moderado",
                "O desempenho pode ser reduzido para controlar a temperatura.",
                Fix("Encerrar apps em segundo plano", FixAction.KILL_BACKGROUND)
            )
        }
        val days = s.uptimeMs / (24 * HOUR)
        if (days >= 3) f += Finding(
            "uptime", Category.PERFORMANCE, if (days >= 7) Severity.WARNING else Severity.INFO,
            "Ligado há $days dias sem reiniciar",
            "Reiniciar limpa vazamentos de memória e processos travados. Recomendado ao menos 1x por semana. " +
                "Segure o botão liga/desliga e toque em Reiniciar."
        )
        if (s.alwaysFinishActivities) f += Finding(
            "dev_finish", Category.PERFORMANCE, Severity.WARNING,
            "\"Não manter atividades\" está ativado",
            "Essa opção de desenvolvedor fecha cada tela ao sair dela, deixando tudo lento e instável.",
            Fix("Desativar", FixAction.DISABLE_ALWAYS_FINISH)
        )
        if (s.animatorScale >= 1f || s.windowScale >= 1f || s.transitionScale >= 1f) f += Finding(
            "anim", Category.PERFORMANCE, Severity.INFO,
            "Animações em velocidade normal",
            "Reduzir as animações para 0,5x deixa a navegação visivelmente mais ágil em aparelhos modestos.",
            Fix("Reduzir para 0,5x", FixAction.REDUCE_ANIMATIONS)
        )
        if (s.powerSaveMode && i.battery.levelPercent > 50) f += Finding(
            "powersave", Category.PERFORMANCE, Severity.INFO,
            "Economia de energia ativa com ${i.battery.levelPercent}% de bateria",
            "O modo economia limita a CPU e a sincronização. Se notar lentidão, desative-o.",
            Fix("Abrir economia de bateria", FixAction.OPEN_BATTERY_SAVER)
        )
    }

    private fun storage(i: Input, f: MutableList<Finding>) {
        val st = i.storage
        val free = st.freeFraction
        if (free < 0.05f || st.availBytes < 2 * GB) f += Finding(
            "sto_low", Category.STORAGE, Severity.CRITICAL,
            "Armazenamento quase cheio (${Format.bytes(st.availBytes)} livres)",
            "Com pouco espaço o sistema não consegue gravar cache e atualizações — gera travamentos " +
                "e lentidão generalizada. Mantenha pelo menos 15% livre.",
            Fix("Liberar espaço", FixAction.OPEN_STORAGE)
        ) else if (free < 0.15f) f += Finding(
            "sto_low", Category.STORAGE, Severity.WARNING,
            "Pouco armazenamento livre (${Format.pct(free)})",
            "${Format.bytes(st.availBytes)} livres de ${Format.bytes(st.totalBytes)}.",
            Fix("Liberar espaço", FixAction.OPEN_STORAGE)
        )
        val totalCache = i.caches?.sumOf { it.cacheBytes } ?: 0L
        if (totalCache >= GB / 2) f += Finding(
            "cache", Category.STORAGE, if (totalCache >= 2 * GB) Severity.WARNING else Severity.INFO,
            "${Format.bytes(totalCache)} em cache de apps",
            "Maiores: " + i.caches!!.sortedByDescending { it.cacheBytes }.take(3)
                .joinToString { "${it.label} (${Format.bytes(it.cacheBytes)})" },
            Fix("Limpar cache de todos os apps", FixAction.CLEAR_ALL_CACHE)
        )
        if (i.unused.size >= 5) {
            val size = i.unused.sumOf { it.sizeBytes ?: 0L }
            f += Finding(
                "unused", Category.APPS, Severity.INFO,
                "${i.unused.size} apps sem uso há 30+ dias",
                "Ocupam ${if (size > 0) Format.bytes(size) else "espaço"} e podem rodar em segundo plano. " +
                    "Veja a aba Apps para desinstalar."
            )
        }
    }

    private fun stability(i: Input, f: MutableList<Finding>) {
        val c = i.crashes ?: return
        c.apps.filter { it.total >= 3 }.take(5).forEach { r ->
            val parts = buildList {
                if (r.crashes > 0) add("${r.crashes} fechamento(s) inesperado(s)")
                if (r.anrs > 0) add("${r.anrs} \"não responde\"")
                if (r.nativeCrashes > 0) add("${r.nativeCrashes} falha(s) nativa(s)")
            }.joinToString(", ")
            f += Finding(
                "crash_${r.packageName}", Category.STABILITY,
                if (r.total >= 10) Severity.CRITICAL else Severity.WARNING,
                "${r.label} travou ${r.total}x em ${c.days} dias",
                "$parts. Atualize o app, limpe o cache dele ou reinstale. Se persistir, desinstale.",
                Fix("Abrir configurações do app", FixAction.OPEN_APP_DETAILS, r.packageName)
            )
        }
        if (c.systemEvents > 0) f += Finding(
            "crash_system", Category.STABILITY,
            if (c.systemEvents >= 3) Severity.CRITICAL else Severity.WARNING,
            "${c.systemEvents} falha(s) do sistema em ${c.days} dias",
            "Reinícios/congelamentos do próprio Android. Atualize o sistema; se continuar, " +
                "considere backup e restauração de fábrica.",
            Fix("Verificar atualização", FixAction.OPEN_SYSTEM_UPDATE)
        )
    }

    private fun security(i: Input, f: MutableList<Finding>) {
        val age = i.system.patchAgeMonths ?: return
        if (age >= 6) f += Finding(
            "patch", Category.SECURITY, if (age >= 12) Severity.WARNING else Severity.INFO,
            "Atualização de segurança de $age meses atrás",
            "Patch: ${i.system.securityPatch}. Atualizações também corrigem bugs de desempenho e bateria.",
            Fix("Verificar atualização", FixAction.OPEN_SYSTEM_UPDATE)
        )
    }

    private fun permissions(i: Input, f: MutableList<Finding>) {
        if (!i.permissions.usageAccess) f += Finding(
            "perm_usage", Category.APPS, Severity.WARNING,
            "Análise de apps desativada",
            "Permita o \"Acesso ao uso\" para identificar apps que drenam bateria, caches grandes e apps sem uso.",
            Fix("Permitir acesso", FixAction.GRANT_USAGE_ACCESS)
        )
    }
}
