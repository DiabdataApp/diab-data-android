package com.diabdata.feature.settings.dataSettingsSection;

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.UserPreferences
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.diabdata.feature.settings.dataSettingsSection.workers.BackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        Log.d("BackupDebug", "setAutoBackupEnabled called with enabled=$enabled")
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
            if (enabled) {
                val frequency = BackupFrequency.fromKey(preferences.value?.frequency ?: "weekly")
                Log.d("BackupDebug", "About to schedule with frequency=${frequency.days} days, prefs=${preferences.value}")
                scheduleBackupWorker(frequency)
            } else {
                Log.d("BackupDebug", "Cancelling backup worker")
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
        Log.d("BackupDebug", "scheduleBackupWorker ENTER, frequency=$frequency")

        val operation = BackupScheduler.scheduleFromUser(application, frequency)

        Log.d("BackupDebug", "enqueueUniquePeriodicWork called, operation=$operation")

        operation.result.addListener({
            try {
                operation.result.get()
                Log.d("BackupDebug", "Enqueue SUCCEEDED")
            } catch (e: Exception) {
                Log.e("BackupDebug", "Enqueue FAILED", e)
            }
        }, { it.run() })
    }

    private fun cancelBackupWorker() {
        BackupScheduler.cancel(application)
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