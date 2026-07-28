package com.diabdata.feature.settings.sections.dataSettings

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.UserPreferences
import com.diabdata.feature.settings.sections.dataSettings.workers.BackupScheduler
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.diabdata.shared.utils.utils.uriStringToReadablePath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: DataRepository, private val application: Application
) : ViewModel() {
    private val backupScheduleMutex: Mutex = Mutex()

    val preferences: StateFlow<UserPreferences?> = repository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private suspend fun applyBackupSchedule(
        enabled: Boolean, frequency: BackupFrequency
    ): Result<Unit> {
        Log.d(
            "BackupDebug", "applyBackupSchedule called with enabled=$enabled, frequency=$frequency"
        )
        return backupScheduleMutex.withLock {
            try {
                Log.d("BackupDebug", "applyBackupSchedule cancelling previous backup worker")
                BackupScheduler.cancel(application).result.await()

                if (enabled) {
                    Log.d("BackupDebug", "applyBackupSchedule scheduling new backup worker")
                    BackupScheduler.scheduleFromUser(application, frequency).result.await()
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BackupDebug", "Failed to apply backup schedule", e)
                Result.failure(e)
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        Log.d("BackupDebug", "setAutoBackupEnabled called with enabled=$enabled")
        viewModelScope.launch {
            val frequency = BackupFrequency.fromKey(preferences.value?.frequency ?: "weekly")
            Log.d(
                "BackupDebug",
                "About to schedule with frequency=${frequency.days} days, prefs=${preferences.value}"
            )
            repository.setAutoBackupEnabled(enabled)
            val result = applyBackupSchedule(enabled, frequency)
            result.onFailure {
                Log.e("BackupDebug", "Failed to apply backup schedule", it)
            }
        }
    }

    fun setAutoBackupFrequency(frequency: BackupFrequency) {
        Log.d("BackupDebug", "setAutoBackupFrequency called with frequency=$frequency")
        viewModelScope.launch {
            Log.d(
                "BackupDebug",
                "About to schedule with frequency=${frequency.days} days, prefs=${preferences.value}"
            )
            repository.setBackupFrequency(frequency.key)
            val result =
                applyBackupSchedule(preferences.value?.automaticBackupEnabled ?: false, frequency)
            result.onFailure {
                Log.e("BackupDebug", "Failed to apply backup schedule", it)
            }
        }
    }

    fun setAutoBackupPath(path: String) {
        viewModelScope.launch {
            Log.d("BackupDebug", "setAutoBackupPath called with path=$path")
            val enabled = preferences.value?.automaticBackupEnabled ?: false
            val frequency = BackupFrequency.fromKey(preferences.value?.frequency ?: "weekly")

            Log.d(
                "BackupDebug",
                "Setting up backup path at ${path.uriStringToReadablePath(application)}"
            )
            repository.setBackupPath(path)
            Log.d(
                "BackupDebug",
                "About to schedule with frequency=${frequency.days} days, prefs=${preferences.value}"
            )
            val result = applyBackupSchedule(enabled, frequency)
            result.onFailure {
                Log.e("BackupDebug", "Failed to apply backup schedule", it)
            }
        }
    }

    fun restorePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            Log.d("BackupDebug", "restorePreferences called with preferences=$preferences")
            repository.restorePreferences(preferences)
            Log.d(
                "BackupDebug",
                "About to schedule with frequency=${preferences.frequency} days, prefs=$preferences"
            )
            val result = applyBackupSchedule(
                preferences.automaticBackupEnabled, BackupFrequency.fromKey(preferences.frequency)
            )
            result.onFailure {
                Log.e("BackupDebug", "Failed to apply backup schedule", it)
            }
        }
    }

    fun resetPreferences() {
        Log.d("BackupDebug", "resetPreferences called")
        viewModelScope.launch {
            Log.d("BackupDebug", "About to reset backup preferences")
            repository.resetBackupPreferences()
            Log.d(
                "BackupDebug", "About to schedule with frequency=weekly, prefs=${preferences.value}"
            )
            val result = applyBackupSchedule(false, BackupFrequency.WEEKLY)
            result.onFailure {
                Log.e("BackupDebug", "Failed to apply backup schedule", it)
            }
        }
    }
}
