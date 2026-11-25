package com.saito.saudeaparelho.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import com.saito.saudeaparelho.model.ActionResult
import com.saito.saudeaparelho.model.Fix
import com.saito.saudeaparelho.model.FixAction

/**
 * Executa correções. O Android não permite que apps comuns alterem tudo sozinhos; por isso
 * algumas ações são aplicadas diretamente (quando há permissão) e outras abrem a tela exata
 * do sistema onde o usuário confirma.
 */
object Optimizer {

    const val ADB_GRANT = "adb shell pm grant com.saito.saudeaparelho android.permission.WRITE_SECURE_SETTINGS\n" +
        "adb shell pm grant com.saito.saudeaparelho android.permission.READ_LOGS"

    fun perform(ctx: Context, fix: Fix): ActionResult = when (fix.action) {
        FixAction.KILL_BACKGROUND -> killBackground(ctx)
        FixAction.CLEAR_OWN_CACHE -> clearOwnCache(ctx)
        FixAction.CLEAR_ALL_CACHE -> clearAllCache(ctx)
        FixAction.SET_SCREEN_TIMEOUT -> withWriteSettings(ctx) {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)
            "Tempo para apagar a tela ajustado para 30 segundos."
        }
        FixAction.SET_AUTO_BRIGHTNESS -> withWriteSettings(ctx) {
            Settings.System.putInt(
                ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            "Brilho automático ativado."
        }
        FixAction.REDUCE_ANIMATIONS -> withSecureSettings(ctx) {
            val cr = ctx.contentResolver
            Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
            Settings.Global.putFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 0.5f)
            Settings.Global.putFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE, 0.5f)
            "Animações reduzidas para 0,5x."
        }
        FixAction.DISABLE_ALWAYS_FINISH -> withSecureSettings(ctx) {
            Settings.Global.putInt(ctx.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0)
            "\"Não manter atividades\" desativado."
        }
        FixAction.OPEN_BATTERY_SAVER -> open(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        FixAction.OPEN_BATTERY_OPTIMIZATION -> open(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            "Em \"Todos os apps\", deixe como \"Otimizar\" os apps que não precisam rodar em segundo plano."
        )
        FixAction.OPEN_STORAGE -> open(Intent(StorageManager.ACTION_MANAGE_STORAGE))
        FixAction.OPEN_DISPLAY -> open(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        FixAction.OPEN_DARK_THEME -> open(Intent("android.settings.DARK_THEME_SETTINGS"))
        FixAction.OPEN_LOCATION -> open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        FixAction.OPEN_SYSTEM_UPDATE -> open(Intent("android.settings.SYSTEM_UPDATE_SETTINGS"))
        FixAction.OPEN_DEVELOPER_OPTIONS -> open(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        FixAction.OPEN_APP_DETAILS -> open(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(fix.packageName)),
            "Dica: em \"Bateria\" escolha \"Restrito/Otimizado\"; em \"Armazenamento\" use \"Limpar cache\"."
        )
        FixAction.UNINSTALL_APP -> open(Intent(Intent.ACTION_DELETE, pkgUri(fix.packageName)))
        FixAction.GRANT_USAGE_ACCESS -> open(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            "Encontre \"Saúde do Aparelho\" e ative \"Permitir acesso ao uso\"."
        )
        FixAction.GRANT_WRITE_SETTINGS -> open(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri(ctx.packageName))
        )
        FixAction.GRANT_ALL_FILES -> grantAllFiles(ctx)
    }

    fun killBackground(ctx: Context): ActionResult {
        val am = ctx.getSystemService(ActivityManager::class.java)
            ?: return ActionResult("Serviço indisponível.")
        val before = SystemAnalyzer.memory(ctx).availBytes
        val pkgs = AppAnalyzer(ctx).allPackages.filter { it != ctx.packageName }
        var n = 0
        for (p in pkgs) {
            if (runCatching { am.killBackgroundProcesses(p) }.isSuccess) n++
        }
        System.gc()
        val after = SystemAnalyzer.memory(ctx).availBytes
        val diff = after - before
        return ActionResult(
            "Processos em segundo plano encerrados ($n apps verificados). " +
                if (diff > 0) "RAM liberada: ${Format.bytes(diff)}." else
                    "O sistema já estava gerenciando bem a memória."
        )
    }

    fun clearOwnCache(ctx: Context): ActionResult {
        var size = 0L
        listOfNotNull(ctx.cacheDir, ctx.codeCacheDir, ctx.externalCacheDir).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                size += f.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
                f.deleteRecursively()
            }
        }
        return ActionResult("Cache temporário do app limpo (${Format.bytes(size)}).")
    }

    private fun clearAllCache(ctx: Context): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return open(
                Intent(StorageManager.ACTION_MANAGE_STORAGE),
                "Nesta versão do Android, use a limpeza do sistema ou \"Limpar cache\" em cada app."
            )
        }
        if (!PermissionChecker.check(ctx).allFilesAccess) return grantAllFiles(ctx)
        return ActionResult(
            message = "Confirme na janela do sistema para limpar o cache de todos os apps.",
            intent = Intent(StorageManager.ACTION_CLEAR_APP_CACHE),
            forResult = true
        )
    }

    private fun grantAllFiles(ctx: Context): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return ActionResult("Não necessário nesta versão.")
        return ActionResult(
            "Ative \"Permitir acesso para gerenciar todos os arquivos\" e toque na correção novamente. " +
                "Ela é exigida pelo Android apenas para o assistente de limpeza de cache.",
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkgUri(ctx.packageName))
        )
    }

    private inline fun withWriteSettings(ctx: Context, block: () -> String): ActionResult =
        if (Settings.System.canWrite(ctx)) {
            ActionResult(runCatching(block).getOrElse { "Não foi possível alterar: ${it.message}" })
        } else ActionResult(
            "Permita \"Modificar configurações do sistema\" e toque na correção novamente.",
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri(ctx.packageName))
        )

    private inline fun withSecureSettings(ctx: Context, block: () -> String): ActionResult =
        if (PermissionChecker.check(ctx).writeSecureSettings) {
            ActionResult(runCatching(block).getOrElse { "Não foi possível alterar: ${it.message}" })
        } else ActionResult(
            "Esta correção exige uma permissão especial concedida uma única vez por computador (ADB). " +
                "Veja \"Recursos avançados\" na aba Otimizar. Abrindo as Opções do desenvolvedor para ajuste manual.",
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        )

    private fun open(intent: Intent, message: String? = null) = ActionResult(message, intent)

    private fun pkgUri(pkg: String?): Uri = Uri.parse("package:${pkg.orEmpty()}")
}
