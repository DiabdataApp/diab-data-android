package com.diabdata.feature.settings.dataSettingsSection;

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.UserPreferences
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
        private val repository: DataRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = repository
            .getUserPreferences()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
        }
    }

    fun setFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            repository.setBackupFrequency(frequency.key)
        }
    }

    fun setBackupPath(path: String) {
        viewModelScope.launch {
            repository.setBackupPath(path)
        }
    }

    fun setLastBackupUpdate(date: String) {
        viewModelScope.launch {
            repository.setLastBackupUpdate(date)
        }
    }

    fun restorePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            repository.restorePreferences(preferences)
        }
    }

    fun resetBackupPreferences() {
        viewModelScope.launch {
            repository.resetBackupPreferences()
        }
    }
}