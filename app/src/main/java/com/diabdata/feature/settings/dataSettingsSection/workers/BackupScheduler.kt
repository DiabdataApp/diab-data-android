package com.diabdata.feature.settings.dataSettingsSection.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.diabdata.workers.scheduledBackups.BackupWorker
import java.util.concurrent.TimeUnit

object BackupScheduler {
    const val UNIQUE_WORK_NAME = "auto_backup_periodic"

    private fun buildWorkRequest(frequency: BackupFrequency): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<BackupWorker>(
            30, TimeUnit.MINUTES
        )
            // .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("auto_backup")
            .build()
    }

    fun scheduleFromUser(context: Context, frequency: BackupFrequency): Operation {
        return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildWorkRequest(frequency)
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}