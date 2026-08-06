package com.diabdata.core.backup

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
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

    private fun isBackupPathAccessible(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val uri = path.toUri()
        return application.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    suspend fun applyBackupSchedule(
        enabled: Boolean,
        frequency: BackupFrequency,
        backupPath: String?
    ): Result<Boolean> {
        val canSchedule = enabled && isBackupPathAccessible(backupPath)
        Log.d(
            "BackupScheduleCoordinator", "applyBackupSchedule called with enabled=$enabled, frequency=$frequency"
        )
        return backupScheduleMutex.withLock {
            try {
                Log.d("BackupScheduleCoordinator", "applyBackupSchedule cancelling previous backup worker")
                BackupScheduler.cancel(application).result.await()
                if (canSchedule) {
                    Log.d("BackupScheduleCoordinator", "applyBackupSchedule scheduling new backup worker")
                    BackupScheduler.scheduleFromUser(application, frequency).result.await()
                }
                Result.success(canSchedule)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BackupScheduleCoordinator", "Failed to apply backup schedule", e)
                Result.failure(e)
            }
        }
    }
}