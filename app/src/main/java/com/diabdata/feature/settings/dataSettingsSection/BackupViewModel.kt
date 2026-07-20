package com.diabdata.feature.settings.dataSettingsSection;

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.UserPreferences
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.diabdata.workers.scheduledBackups.BackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
        private val repository: DataRepository,
        private val application: Application
) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = repository
            .getUserPreferences()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
            if (enabled) {
                val frequency = BackupFrequency.fromKey(preferences.value?.frequency ?: "weekly")
                scheduleBackupWorker(frequency)
            } else {
                cancelBackupWorker()
            }
        }
    }

    fun setFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            repository.setBackupFrequency(frequency.key)
            if (preferences.value?.automaticBackupEnabled == true) {
                scheduleBackupWorker(frequency)
            }
        }
    }

    fun setBackupPath(path: String) {
        viewModelScope.launch {
            repository.setBackupPath(path)
            if (preferences.value?.automaticBackupEnabled == true) {
                scheduleBackupWorker(BackupFrequency.fromKey(preferences.value?.frequency ?: "weekly"))
            }
        }
    }

    private fun scheduleBackupWorker(frequency: BackupFrequency) {
        val workManager = WorkManager.getInstance(application)
//        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
//            frequency.days, TimeUnit.DAYS
//        ).setConstraints(
//            Constraints.Builder()
//                .setRequiresBatteryNotLow(true)
//                .setRequiresStorageNotLow(true)
//                .build()
//        ).build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName = "auto_backup_periodic",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request = backupRequest
        )
    }

    private fun cancelBackupWorker() {
        val workManager = WorkManager.getInstance(application)
        workManager.cancelUniqueWork("auto_backup_periodic")
    }

    fun restorePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            cancelBackupWorker()
            repository.restorePreferences(preferences)
            if (preferences.automaticBackupEnabled) {
                scheduleBackupWorker(BackupFrequency.fromKey(preferences.frequency))
            }
        }
    }

    fun resetBackupPreferences() {
        viewModelScope.launch {
            cancelBackupWorker()
            repository.resetBackupPreferences()
        }
    }
}