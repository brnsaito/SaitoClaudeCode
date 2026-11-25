package com.saito.saudeaparelho.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BatterySampleWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { BatteryHistory.record(applicationContext) }
        return Result.success()
    }
}
