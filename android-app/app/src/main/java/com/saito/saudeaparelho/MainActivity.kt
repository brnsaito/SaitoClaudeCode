package com.saito.saudeaparelho

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saito.saudeaparelho.ui.AppsScreen
import com.saito.saudeaparelho.ui.BatteryScreen
import com.saito.saudeaparelho.ui.DashboardScreen
import com.saito.saudeaparelho.ui.OptimizeScreen
import com.saito.saudeaparelho.ui.PerformanceScreen
import com.saito.saudeaparelho.ui.SaudeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SaudeTheme { App() } }
    }
}

private data class Tab(val title: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Início", Icons.Filled.HealthAndSafety),
    Tab("Bateria", Icons.Filled.BatteryChargingFull),
    Tab("Desempenho", Icons.Filled.Speed),
    Tab("Apps", Icons.Filled.Apps),
    Tab("Otimizar", Icons.Filled.AutoFixHigh)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: MainViewModel = viewModel()) {
    val ui by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refresh()
    }

    // Reanalisa sempre que o usuário volta ao app (ex.: depois de conceder uma permissão)
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is UiEvent.Message -> snackbar.showSnackbar(e.text, duration = SnackbarDuration.Long)
                is UiEvent.Launch -> {
                    try {
                        if (e.forResult) resultLauncher.launch(e.intent)
                        else ctx.startActivity(e.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: ActivityNotFoundException) {
                        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: SecurityException) {
                        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (tab == 0) "Saúde do Aparelho" else TABS[tab].title) },
                    actions = {
                        IconButton(onClick = { vm.refresh() }, enabled = !ui.loading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Analisar novamente")
                        }
                    }
                )
                if (ui.loading && ui.report != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val r = ui.report
            if (r == null) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Analisando o aparelho…", Modifier.padding(top = 16.dp))
                }
            } else {
                val onFix = vm::perform
                when (tab) {
                    0 -> DashboardScreen(r, onFix) { tab = 4 }
                    1 -> BatteryScreen(r, onFix)
                    2 -> PerformanceScreen(r, onFix)
                    3 -> AppsScreen(r, onFix)
                    4 -> OptimizeScreen(r, ui, onFix, vm::optimizeAll)
                }
            }
        }
    }
}
