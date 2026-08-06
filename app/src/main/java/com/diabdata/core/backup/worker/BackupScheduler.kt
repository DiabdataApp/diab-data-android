package com.diabdata.core.backup.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import java.util.concurrent.TimeUnit

object BackupScheduler {
    const val AUTO_BACKUP_UNIQUE_WORK_NAME = "auto_backup_periodic"

    private fun buildWorkRequest(frequency: BackupFrequency): PeriodicWorkRequest {
        Log.d("BackupDebug", "Building Periodic work request")
        return PeriodicWorkRequestBuilder<BackupWorker>(
            frequency.days, TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setInitialDelay(frequency.days, TimeUnit.DAYS)
            .addTag("auto_backup")
            .build()
    }

    fun scheduleFromUser(context: Context, frequency: BackupFrequency): Operation {
        Log.d("BackupDebug", "Queuing unique work with name=$AUTO_BACKUP_UNIQUE_WORK_NAME")
        return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AUTO_BACKUP_UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            buildWorkRequest(frequency)
        )
    }

    fun cancel(context: Context): Operation {
        Log.d("BackupDebug", "Cancelling unique work with name=$AUTO_BACKUP_UNIQUE_WORK_NAME")
        return WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_UNIQUE_WORK_NAME)
    }
}