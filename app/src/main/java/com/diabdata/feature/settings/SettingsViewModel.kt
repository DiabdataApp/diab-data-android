package com.diabdata.feature.settings

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.diabdata.core.database.DataRepository
import com.diabdata.core.database.DiabDataDatabase
import com.diabdata.core.model.UserPreferences
import com.diabdata.feature.dataMatrixScanner.utils.MedicalDevicesInitializer
import com.diabdata.feature.dataMatrixScanner.utils.MedicationInitializer
import com.diabdata.workers.reminders.scheduleAppointmentReminders
import com.diabdata.workers.reminders.scheduleMedicationExpirationReminders
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.diabdata.shared.R as shared

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DataRepository,
    private val db: DiabDataDatabase,
) : ViewModel() {
    val isExpirationReminderEnabled = {
        viewModelScope.launch(Dispatchers.IO) {
            repository.isExpirationReminderEnabled()
        }
    }

    val preferences: StateFlow<UserPreferences?> = repository
        .getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun forceRebuildMedicalDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            MedicalDevicesInitializer(context, db).initialize()
        }
    }

    fun forceRebuildMedications() {
        viewModelScope.launch(Dispatchers.IO) {
            MedicationInitializer(context, db).initialize()
        }
    }

    fun onExpirationReminderSwitch(enabled: Boolean) {
        val toastExpirationEnabled = context.getString(shared.string.medications_reminders_enabled_success_toast)
        val workManager = WorkManager.getInstance(context)

        viewModelScope.launch {
            repository.enableExpirationReminder(enabled)
            if (enabled) {
                scheduleMedicationExpirationReminders(context, repository)
                Toast.makeText(context, toastExpirationEnabled, Toast.LENGTH_SHORT)
                    .show()
            } else {
                workManager.cancelAllWorkByTag("treatments")
            }
        }
    }

    fun onAppointmentSwitch(enabled: Boolean) {
        val toastAppointmentReminderEnabled = context.getString(shared.string.appointments_reminders_enabled_success_toast)
        val workManager = WorkManager.getInstance(context)

        viewModelScope.launch {
            repository.enableAppointmentReminder(enabled)
            if (enabled) {
                scheduleAppointmentReminders(context, repository)
                Toast.makeText(context, toastAppointmentReminderEnabled, Toast.LENGTH_SHORT)
                    .show()
            } else {
                workManager.cancelAllWorkByTag("appointments")
            }
        }
    }

    fun enableAppointmentReminder(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.enableAppointmentReminder(enabled)
        }

    fun enableExpirationReminder(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.enableExpirationReminder(enabled)
        }
}