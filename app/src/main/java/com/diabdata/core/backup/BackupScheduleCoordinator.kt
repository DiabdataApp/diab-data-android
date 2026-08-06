package com.diabdata.core.backup

import android.app.Application
import android.util.Log
import com.diabdata.core.backup.worker.BackupScheduler
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupScheduleCoordinator @Inject constructor(
    private val application: Application
) {
    private val backupScheduleMutex: Mutex = Mutex()

    suspend fun applyBackupSchedule(
        enabled: Boolean, frequency: BackupFrequency
    ): Result<Unit> {
        Log.d(
            "BackupScheduleCoordinator", "applyBackupSchedule called with enabled=$enabled, frequency=$frequency"
        )
        return backupScheduleMutex.withLock {
            try {
                Log.d("BackupScheduleCoordinator", "applyBackupSchedule cancelling previous backup worker")
                BackupScheduler.cancel(application).result.await()
                if (enabled) {
                    Log.d("BackupScheduleCoordinator", "applyBackupSchedule scheduling new backup worker")
                    BackupScheduler.scheduleFromUser(application, frequency).result.await()
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BackupScheduleCoordinator", "Failed to apply backup schedule", e)
                Result.failure(e)
            }
        }
    }
}