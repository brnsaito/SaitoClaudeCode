package com.saito.saudeaparelho

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.saito.saudeaparelho.data.BatterySampleWorker
import java.util.concurrent.TimeUnit

class SaudeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Coleta o nível da bateria a cada ~15 min para medir a descarga real
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "battery_sampler",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BatterySampleWorker>(15, TimeUnit.MINUTES).build()
        )
    }
}
